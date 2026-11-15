package es.alimentarse.alimentacion;

import static java.util.stream.Collectors.counting;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toSet;

import es.alimentarse.catalogo.CatalogoMapper;
import es.alimentarse.catalogo.GrupoDto;
import es.alimentarse.comun.error.ExcepcionApi;
import es.alimentarse.consumo.ConsumoDto;
import es.alimentarse.consumo.ConsumoRequest;
import es.alimentarse.consumo.ConsumoService;
import es.alimentarse.receta.RecetaResumenDto;
import es.alimentarse.receta.RecetaService;
import java.text.Collator;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Planificador semanal (D-068): comidas futuras y cómo quedarían las raciones si se cumple el plan. Cada usuario solo
 * ve y toca las suyas; las ajenas son un 404, como los consumos (D-039).
 */
@Service
public class PlanService {

    /** Hasta dónde se puede planificar. */
    static final Duration HORIZONTE = Duration.ofDays(90);

    /** Intervalo máximo que se puede pedir de una vez. */
    static final Duration INTERVALO_MAXIMO = Duration.ofDays(31);

    /** Comidas planificadas que aún no han llegado, como mucho (D-089): más de tres meses de comidas y cenas. */
    static final int MAXIMO_PENDIENTES = 200;

    static final String DEMASIADAS_PENDIENTES =
            "Ya tienes 200 comidas planificadas pendientes: quita alguna para añadir otra.";

    /** Desfase de reloj del cliente que se tolera antes de considerar que un momento ya ha pasado. */
    static final Duration MARGEN_RELOJ = Duration.ofMinutes(1);

    static final String FECHA_PASADA = "Ese momento ya ha pasado";
    static final String FECHA_LEJANA = "Solo se puede planificar hasta 90 días vista";
    static final String HUECO_OCUPADO = "Ya tienes una receta planificada para ese momento.";

    private final PlanMapper plan;
    private final AlimentacionMapper alimentacion;
    private final CatalogoMapper catalogo;
    private final RecetaService recetas;
    private final ConsumoService consumos;
    private final Clock reloj;

    public PlanService(
            PlanMapper plan,
            AlimentacionMapper alimentacion,
            CatalogoMapper catalogo,
            RecetaService recetas,
            ConsumoService consumos,
            Clock reloj) {
        this.plan = plan;
        this.alimentacion = alimentacion;
        this.catalogo = catalogo;
        this.recetas = recetas;
        this.consumos = consumos;
        this.reloj = reloj;
    }

    /** Las comidas del intervalo y cómo quedarían los grupos al final de él. */
    @Transactional(readOnly = true)
    public PlanDto plan(Instant desde, Instant hasta, String yo) {
        exigirIntervalo(desde, hasta);
        return construir(desde, hasta, yo);
    }

    /**
     * Lista de la compra (D-076): los ingredientes de las comidas del intervalo que aún no han pasado, por grupo de
     * alimentos. Las comidas cuya receta ya no está disponible no entran, pero se cuentan para avisar.
     */
    @Transactional(readOnly = true)
    public ListaCompraDto listaCompra(Instant desde, Instant hasta, String yo) {
        exigirIntervalo(desde, hasta);
        Instant ahora = reloj.instant();
        List<ComidaPlanificada> futuras = plan.comidas(yo, desde.isAfter(ahora) ? desde : ahora, hasta).stream()
                .filter(comida -> comida.fecha().isAfter(ahora))
                .toList();
        Map<Integer, RecetaResumenDto> tarjetas = recetas.resumenes(
                futuras.stream().map(ComidaPlanificada::receta).collect(toSet()), yo);
        List<ComidaPlanificada> incluidas = futuras.stream()
                .filter(comida -> {
                    RecetaResumenDto receta = tarjetas.get(comida.receta());
                    return receta != null && disponible(receta, yo);
                })
                .toList();
        Map<Integer, Long> veces = incluidas.stream().collect(groupingBy(ComidaPlanificada::receta, counting()));
        List<IngredienteDeReceta> ingredientes =
                veces.isEmpty() ? List.of() : plan.ingredientesDeRecetas(veces.keySet());
        List<ComidaCompraDto> comidas = incluidas.stream()
                .map(comida -> new ComidaCompraDto(
                        comida.id(),
                        comida.fecha(),
                        comida.receta(),
                        tarjetas.get(comida.receta()).titulo()))
                .toList();
        return new ListaCompraDto(comidas, secciones(ingredientes, veces, tarjetas), futuras.size() - incluidas.size());
    }

    /** Solo recetas que el usuario puede ver y momentos futuros; un hueco admite una sola receta. */
    @Transactional
    public ComidaPlanificadaDto anadir(ComidaPlanRequest datos, String yo) {
        recetas.exigirVisible(datos.receta(), yo);
        Instant fecha = fechaValida(datos.fecha());
        if (plan.existeHueco(yo, fecha)) {
            throw ExcepcionApi.conflicto("HUECO_OCUPADO", HUECO_OCUPADO, null);
        }
        if (plan.contarPendientes(yo, reloj.instant()) >= MAXIMO_PENDIENTES) {
            throw ExcepcionApi.conflicto("LIMITE_PLANIFICADAS", DEMASIADAS_PENDIENTES, null);
        }
        plan.insertar(yo, datos.receta(), fecha);
        return construir(fecha, fecha, yo).comidas().getFirst();
    }

    @Transactional
    public void borrar(long id, String yo) {
        propia(id, yo);
        plan.borrar(id);
    }

    /**
     * «Ya me la he comido»: pasa la comida a los consumos y la quita del plan. Si aún no ha llegado su momento, el
     * consumo es de ahora, porque un consumo no puede ser futuro (6.7).
     */
    @Transactional
    public ConsumoDto consumir(long id, String yo) {
        ComidaPlanificada comida = propia(id, yo);
        Instant ahora = reloj.instant();
        Instant fecha = comida.fecha().isAfter(ahora) ? ahora : comida.fecha();
        ConsumoDto consumo = consumos.registrar(new ConsumoRequest(comida.receta(), fecha), yo);
        plan.borrar(id);
        return consumo;
    }

    private PlanDto construir(Instant desde, Instant hasta, String yo) {
        Instant ahora = reloj.instant();
        // Una comida del intervalo cuenta lo que haya en los 7 días anteriores, aunque quede fuera de él
        Instant inicio = desde.minus(RecomendacionService.VENTANA);
        List<ComidaPlanificada> comidas = plan.comidas(yo, inicio, hasta);
        Set<Integer> ids = comidas.stream().map(ComidaPlanificada::receta).collect(toSet());
        Map<Integer, RecetaResumenDto> tarjetas = recetas.resumenes(ids, yo);
        Map<Integer, List<GrupoDeReceta>> porReceta = ids.isEmpty()
                ? Map.of()
                : alimentacion.gruposDeRecetas(ids).stream().collect(groupingBy(GrupoDeReceta::receta));

        List<ConsumoGrupo> filas = new ArrayList<>(alimentacion.consumosConGrupos(yo, inicio, ahora));
        comidas.stream()
                .filter(comida -> cuenta(comida, tarjetas.get(comida.receta()), ahora, yo))
                // Id negativo: una comida planificada no coincide nunca con el id de un consumo (6.2 cuenta ids
                // distintos)
                .forEach(comida -> porReceta
                        .getOrDefault(comida.receta(), List.of())
                        .forEach(g -> filas.add(new ConsumoGrupo(-comida.id(), comida.fecha(), g.grupoId()))));

        List<GrupoDto> grupos = catalogo.grupos();
        List<ComidaPlanificadaDto> delIntervalo = comidas.stream()
                .filter(comida -> !comida.fecha().isBefore(desde) && tarjetas.containsKey(comida.receta()))
                .map(comida -> aDto(comida, tarjetas.get(comida.receta()), porReceta, filas, grupos, ahora, yo))
                .toList();
        return new PlanDto(
                delIntervalo,
                RecomendacionService.indicadores(RecomendacionService.racionesPorGrupo(filas, hasta), grupos));
    }

    private static ComidaPlanificadaDto aDto(
            ComidaPlanificada comida,
            RecetaResumenDto receta,
            Map<Integer, List<GrupoDeReceta>> porReceta,
            List<ConsumoGrupo> filas,
            List<GrupoDto> grupos,
            Instant ahora,
            String yo) {
        Set<Integer> gruposDeLaComida = porReceta.getOrDefault(comida.receta(), List.of()).stream()
                .map(GrupoDeReceta::grupoId)
                .collect(toSet());
        List<IndicadorGrupoDto> excesos = cuenta(comida, receta, ahora, yo)
                ? RecomendacionService.excesos(filas, comida.fecha(), gruposDeLaComida, grupos)
                : List.of();
        return new ComidaPlanificadaDto(comida.id(), comida.fecha(), receta, disponible(receta, yo), excesos);
    }

    /** Lo ya pasado no cuenta como comido si no se ha registrado: los consumos son la verdad (D-068). */
    private static boolean cuenta(ComidaPlanificada comida, RecetaResumenDto receta, Instant ahora, String yo) {
        return receta != null && comida.fecha().isAfter(ahora) && disponible(receta, yo);
    }

    /** Las mismas reglas que {@code RecetaBase.visiblePara}, sobre la tarjeta. */
    private static boolean disponible(RecetaResumenDto receta, String yo) {
        return !receta.eliminada() && (yo.equals(receta.autor()) || (receta.publicada() && !receta.oculta()));
    }

    /**
     * Los ingredientes por grupo de alimentos, con los grupos básicos al final, y en cada grupo por nombre. De cada
     * ingrediente, lo que pide cada receta y el total, si se puede sumar ({@link CantidadesCompra}).
     */
    static List<SeccionCompraDto> secciones(
            List<IngredienteDeReceta> ingredientes, Map<Integer, Long> veces, Map<Integer, RecetaResumenDto> tarjetas) {
        Collator orden = Collator.getInstance(Locale.forLanguageTag("es"));
        orden.setStrength(Collator.PRIMARY);
        return ingredientes.stream().collect(groupingBy(IngredienteDeReceta::grupoId)).values().stream()
                .map(delGrupo -> new SeccionCompraDto(
                        delGrupo.getFirst().grupo(),
                        delGrupo.getFirst().basico(),
                        articulos(delGrupo, veces, tarjetas, orden)))
                .sorted(Comparator.comparing(SeccionCompraDto::basico)
                        .thenComparing(seccion -> seccion.grupo().nombre(), orden))
                .toList();
    }

    private static List<ArticuloCompraDto> articulos(
            List<IngredienteDeReceta> delGrupo,
            Map<Integer, Long> veces,
            Map<Integer, RecetaResumenDto> tarjetas,
            Collator orden) {
        return delGrupo.stream().collect(groupingBy(IngredienteDeReceta::ingrediente)).values().stream()
                .map(filas -> {
                    List<CantidadCompraDto> cantidades = filas.stream()
                            .map(fila -> new CantidadCompraDto(
                                    fila.receta(),
                                    tarjetas.get(fila.receta()).titulo(),
                                    fila.cantidad(),
                                    veces.get(fila.receta()).intValue()))
                            .sorted(Comparator.comparing(CantidadCompraDto::titulo, orden))
                            .toList();
                    IngredienteDeReceta primera = filas.getFirst();
                    return new ArticuloCompraDto(
                            primera.ingrediente(), primera.nombre(), CantidadesCompra.total(cantidades), cantidades);
                })
                .sorted(Comparator.comparing(ArticuloCompraDto::nombre, orden))
                .toList();
    }

    private static void exigirIntervalo(Instant desde, Instant hasta) {
        if (!hasta.isAfter(desde) || Duration.between(desde, hasta).compareTo(INTERVALO_MAXIMO) > 0) {
            throw ExcepcionApi.peticionIncorrecta("VALIDACION", "El intervalo de fechas no es válido.", Map.of());
        }
    }

    /** Sin fracciones de segundo (la columna es {@code TIMESTAMP}). */
    private Instant fechaValida(Instant fecha) {
        Instant ahora = reloj.instant();
        if (fecha.isBefore(ahora.minus(MARGEN_RELOJ))) {
            throw errorDeFecha(FECHA_PASADA);
        }
        if (fecha.isAfter(ahora.plus(HORIZONTE))) {
            throw errorDeFecha(FECHA_LEJANA);
        }
        return fecha.truncatedTo(ChronoUnit.SECONDS);
    }

    private static ExcepcionApi errorDeFecha(String mensaje) {
        return ExcepcionApi.peticionIncorrecta(
                "VALIDACION", "Revisa los datos del formulario.", Map.of("fecha", mensaje));
    }

    /** Con bloqueo: dos peticiones sobre la misma comida no la consumen dos veces. */
    private ComidaPlanificada propia(long id, String yo) {
        return plan.bloquear(id)
                .filter(comida -> comida.esDe(yo))
                .orElseThrow(() -> ExcepcionApi.noEncontrado("Esa comida no está en tu planificador."));
    }
}
