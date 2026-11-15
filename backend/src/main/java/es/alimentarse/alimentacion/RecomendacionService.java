package es.alimentarse.alimentacion;

import static java.util.stream.Collectors.collectingAndThen;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.toSet;

import es.alimentarse.catalogo.CatalogoMapper;
import es.alimentarse.catalogo.GrupoDto;
import es.alimentarse.comun.Pagina;
import es.alimentarse.receta.FiltroRecetas;
import es.alimentarse.receta.RecetaResumenDto;
import es.alimentarse.receta.RecetaService;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Reglas de alimentación de la especificación, sobre los últimos 7 días del usuario:
 *
 * <ul>
 *   <li>6.2: raciones consumidas por grupo;
 *   <li>6.3: grupos agotados y recetas recomendadas («¿Qué toca hoy?»);
 *   <li>6.4: indicadores circulares de «Mi alimentación».
 * </ul>
 *
 * Además, lo que hace más útil la recomendación (D-067): favorecer la variedad, explicar cada receta y avisar de lo que
 * pasaría al comerla.
 *
 * Las reglas son funciones puras (estáticas) para poder probarlas exhaustivamente sin base de datos.
 */
@Service
public class RecomendacionService {

    /** Ventana de la regla 6.2: de ahora − 7 días a ahora, ambos incluidos (D-038). */
    static final Duration VENTANA = Duration.ofDays(7);

    static final double UMBRAL_AMARILLO = 0.75;

    /** Primero los que tienen límite, de más a menos llenos, y después los ilimitados; a igualdad, por nombre (D-040). */
    private static final Comparator<IndicadorGrupoDto> ORDEN_INDICADORES = Comparator.comparing(
                    (IndicadorGrupoDto i) -> i.limite() == null)
            .thenComparing(RecomendacionService::proporcion, Comparator.reverseOrder())
            .thenComparing(i -> i.grupo().nombre());

    private final AlimentacionMapper alimentacion;
    private final CatalogoMapper catalogo;
    private final RecetaService recetas;
    private final Clock reloj;

    public RecomendacionService(
            AlimentacionMapper alimentacion, CatalogoMapper catalogo, RecetaService recetas, Clock reloj) {
        this.alimentacion = alimentacion;
        this.catalogo = catalogo;
        this.recetas = recetas;
        this.reloj = reloj;
    }

    /**
     * «¿Qué toca hoy?» (6.3): recetas publicadas que cumplen los filtros y no tienen ningún ingrediente de un grupo
     * agotado. Las de los grupos que menos ha comido esta semana van primero y, a igualdad, por «me gusta» y fecha de
     * creación (D-067).
     */
    @Transactional(readOnly = true)
    public Pagina<RecetaRecomendadaDto> recomendadas(FiltroRecetas filtro, String yo, int pagina, int tamano) {
        Map<Integer, Long> raciones = racionesUltimos7Dias(yo);
        List<GrupoDto> grupos = catalogo.grupos();
        Pagina<RecetaResumenDto> encontradas = recetas.buscarPaginaPorVariedad(
                filtro.soloLasPublicadas().sinGrupos(gruposAgotados(raciones, grupos)),
                yo,
                gruposConsumidos(raciones, grupos),
                gruposCercaDelLimite(raciones, grupos),
                pagina,
                tamano);
        if (encontradas.contenido().isEmpty()) {
            return encontradas.map(receta -> new RecetaRecomendadaDto(receta, List.of()));
        }
        List<Integer> ids =
                encontradas.contenido().stream().map(RecetaResumenDto::id).toList();
        Map<Integer, List<GrupoDeReceta>> porReceta =
                alimentacion.gruposDeRecetas(ids).stream().collect(groupingBy(GrupoDeReceta::receta));
        return encontradas.map(receta -> new RecetaRecomendadaDto(
                receta, estadoDeSusGrupos(porReceta.getOrDefault(receta.id(), List.of()), raciones)));
    }

    /** Qué pasaría con los grupos con límite de la receta si el usuario la comiera ahora (D-067). */
    @Transactional(readOnly = true)
    public List<ImpactoGrupoDto> impacto(int receta, String yo) {
        recetas.exigirVisible(receta, yo);
        return impacto(alimentacion.gruposDeRecetas(List.of(receta)), racionesUltimos7Dias(yo));
    }

    /** Indicadores de los grupos consumidos por el usuario en los últimos 7 días (6.4). */
    @Transactional(readOnly = true)
    public List<IndicadorGrupoDto> indicadores(String usuario) {
        return indicadores(racionesUltimos7Dias(usuario), catalogo.grupos());
    }

    private Map<Integer, Long> racionesUltimos7Dias(String usuario) {
        Instant ahora = reloj.instant();
        return racionesPorGrupo(alimentacion.consumosConGrupos(usuario, ahora.minus(VENTANA), ahora), ahora);
    }

    /**
     * Regla 6.2: raciones de cada grupo = número de consumos distintos dentro de la ventana cuya receta tiene algún
     * ingrediente del grupo. Solo aparecen los grupos con al menos una ración.
     */
    static Map<Integer, Long> racionesPorGrupo(Collection<ConsumoGrupo> filas, Instant ahora) {
        Instant desde = ahora.minus(VENTANA);
        return filas.stream()
                .filter(fila -> !fila.fecha().isBefore(desde) && !fila.fecha().isAfter(ahora))
                .collect(groupingBy(
                        ConsumoGrupo::grupo, collectingAndThen(mapping(ConsumoGrupo::consumo, toSet()), consumos ->
                                (long) consumos.size())));
    }

    /** Regla 6.3: un grupo está agotado si tiene límite y ya se han consumido al menos esas raciones. */
    static Set<Integer> gruposAgotados(Map<Integer, Long> raciones, Collection<GrupoDto> grupos) {
        return grupos.stream()
                .filter(grupo -> grupo.racionesSemana() != null
                        && raciones.getOrDefault(grupo.id(), 0L) >= grupo.racionesSemana())
                .map(GrupoDto::id)
                .collect(toSet());
    }

    /**
     * Regla 6.4: un indicador por cada grupo con raciones. Primero los que tienen límite, de más a menos lleno, y
     * después los ilimitados; a igualdad, por nombre (D-040).
     */
    static List<IndicadorGrupoDto> indicadores(Map<Integer, Long> raciones, Collection<GrupoDto> grupos) {
        return grupos.stream()
                .filter(grupo -> raciones.getOrDefault(grupo.id(), 0L) > 0)
                .map(grupo -> indicador(grupo, raciones.get(grupo.id())))
                .sorted(ORDEN_INDICADORES)
                .toList();
    }

    /** Grupos con límite de los que ya se ha comido algo esta semana (D-067). */
    static Set<Integer> gruposConsumidos(Map<Integer, Long> raciones, Collection<GrupoDto> grupos) {
        return grupos.stream()
                .filter(grupo -> grupo.racionesSemana() != null && raciones.getOrDefault(grupo.id(), 0L) > 0)
                .map(GrupoDto::id)
                .collect(toSet());
    }

    /** Grupos con el indicador en ámbar o en rojo: los que peor le vienen a la variedad (D-067). */
    static Set<Integer> gruposCercaDelLimite(Map<Integer, Long> raciones, Collection<GrupoDto> grupos) {
        return grupos.stream()
                .filter(grupo ->
                        indicador(grupo, raciones.getOrDefault(grupo.id(), 0L)).color() != ColorIndicador.VERDE)
                .map(GrupoDto::id)
                .collect(toSet());
    }

    /** Cómo van esta semana los grupos de una receta, también los que aún no se han comido. */
    static List<IndicadorGrupoDto> estadoDeSusGrupos(Collection<GrupoDeReceta> filas, Map<Integer, Long> raciones) {
        return filas.stream()
                .filter(GrupoDeReceta::relevante)
                .map(fila -> indicador(fila.grupo(), raciones.getOrDefault(fila.grupoId(), 0L)))
                .sorted(ORDEN_INDICADORES)
                .toList();
    }

    /**
     * Planificador (D-068): grupos de una comida que, ese momento y contándola a ella, superarían su límite semanal.
     * Llegar justo al límite no es un exceso.
     */
    static List<IndicadorGrupoDto> excesos(
            Collection<ConsumoGrupo> filas,
            Instant momento,
            Collection<Integer> gruposDeLaComida,
            Collection<GrupoDto> grupos) {
        return indicadores(racionesPorGrupo(filas, momento), grupos).stream()
                .filter(i -> i.limite() != null
                        && i.consumidas() > i.limite()
                        && gruposDeLaComida.contains(i.grupo().id()))
                .toList();
    }

    /** Una ración más en cada grupo con límite de la receta (una receta suma una sola ración por grupo, 6.2). */
    static List<ImpactoGrupoDto> impacto(Collection<GrupoDeReceta> filas, Map<Integer, Long> raciones) {
        return filas.stream()
                .filter(fila -> fila.racionesSemana() != null)
                .map(fila -> {
                    long consumidas = raciones.getOrDefault(fila.grupoId(), 0L);
                    return new ImpactoGrupoDto(
                            indicador(fila.grupo(), consumidas), indicador(fila.grupo(), consumidas + 1));
                })
                .sorted(Comparator.comparing(ImpactoGrupoDto::despues, ORDEN_INDICADORES))
                .toList();
    }

    static IndicadorGrupoDto indicador(GrupoDto grupo, long consumidas) {
        Integer limite = grupo.racionesSemana();
        if (limite == null) {
            return new IndicadorGrupoDto(grupo, consumidas, null, 100, ColorIndicador.VERDE);
        }
        double proporcion = (double) consumidas / limite;
        int porcentaje = (int) Math.min(100, Math.round(proporcion * 100));
        ColorIndicador color = proporcion >= 1
                ? ColorIndicador.ROJO
                : proporcion >= UMBRAL_AMARILLO ? ColorIndicador.AMARILLO : ColorIndicador.VERDE;
        return new IndicadorGrupoDto(grupo, consumidas, limite, porcentaje, color);
    }

    private static double proporcion(IndicadorGrupoDto indicador) {
        return indicador.limite() == null ? 0 : (double) indicador.consumidas() / indicador.limite();
    }
}
