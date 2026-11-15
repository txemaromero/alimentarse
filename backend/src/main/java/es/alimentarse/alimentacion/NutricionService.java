package es.alimentarse.alimentacion;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toMap;

import es.alimentarse.receta.RecetaService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Stream;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Estudio nutricional de una receta (D-079): suma la composición de BEDCA (por 100 g de porción comestible) de los
 * ingredientes cuya cantidad se puede convertir a gramos. Es una estimación: las cantidades se toman como porción
 * comestible, 1 ml cuenta como 1 g y las cucharas y las piezas tienen pesos aproximados. Las funciones estáticas son
 * puras.
 */
@Service
public class NutricionService {

    /** 1 kcal = 4,184 kJ. */
    public static final BigDecimal KJ_POR_KCAL = new BigDecimal("4.184");

    /** Sal = sodio × 2,5 (Reglamento (UE) n.º 1169/2011, anexo I). */
    static final BigDecimal SAL_POR_SODIO = new BigDecimal("2.5");

    /** Pieza que se supone cuando la cantidad es solo un número («2» huevos). */
    static final String PIEZA_POR_DEFECTO = "unidad";

    /**
     * Gramos de cada unidad que vale para cualquier ingrediente: de masa, de volumen (1 ml ≈ 1 g) y cucharas (una
     * cucharada sopera son 15 ml, y una cucharadita, 5).
     */
    static final Map<String, BigDecimal> UNIDADES = Stream.of(
                    unidades(1, "g", "gr", "grs", "gramo", "gramos", "ml", "mililitro", "mililitros"),
                    unidades(10, "cl", "centilitro", "centilitros"),
                    unidades(100, "dl", "decilitro", "decilitros"),
                    unidades(1000, "kg", "kilo", "kilos", "kilogramo", "kilogramos", "l", "litro", "litros"),
                    unidades(15, "cucharada", "cucharadas"),
                    unidades(5, "cucharadita", "cucharaditas"))
            .flatMap(Function.identity())
            .collect(toMap(Map.Entry::getKey, Map.Entry::getValue));

    /**
     * Ingestas de referencia de un adulto medio por día (Reglamento (UE) n.º 1169/2011, anexo XIII): energía en kJ, sal
     * en g y el resto en g.
     */
    static final BigDecimal REFERENCIA_KJ = BigDecimal.valueOf(8400);

    static final BigDecimal REFERENCIA_GRASAS = BigDecimal.valueOf(70);
    static final BigDecimal REFERENCIA_SATURADAS = BigDecimal.valueOf(20);
    static final BigDecimal REFERENCIA_HIDRATOS = BigDecimal.valueOf(260);
    static final BigDecimal REFERENCIA_PROTEINAS = BigDecimal.valueOf(50);
    static final BigDecimal REFERENCIA_SAL = BigDecimal.valueOf(6);

    private static final BigDecimal CIEN = BigDecimal.valueOf(100);
    private static final BigDecimal MIL = BigDecimal.valueOf(1000);
    private static final int PRECISION = 6;

    private final RecetaService recetas;
    private final NutricionMapper nutricion;
    private final Clock reloj;

    public NutricionService(RecetaService recetas, NutricionMapper nutricion, Clock reloj) {
        this.recetas = recetas;
        this.nutricion = nutricion;
        this.reloj = reloj;
    }

    /** La estimación de una receta que el usuario puede ver (404 si no); los administradores, también las ocultas. */
    @Transactional(readOnly = true)
    public NutricionRecetaDto deReceta(int receta, String yo, boolean admin) {
        recetas.exigirVisible(receta, yo, admin);
        List<Integer> una = List.of(receta);
        int comensales = nutricion.comensales(una).getFirst().comensales();
        return calcular(comensales, nutricion.ingredientes(una), piezasPorIngrediente(una));
    }

    /**
     * Nutrientes de lo comido en los últimos 7 días, la misma ventana que los indicadores (6.4): una ración de la
     * receta de cada consumo (D-082).
     */
    @Transactional(readOnly = true)
    public NutricionSemanaDto semana(String usuario) {
        Instant ahora = reloj.instant();
        List<Integer> consumos = nutricion.recetasConsumidas(usuario, ahora.minus(RecomendacionService.VENTANA), ahora);
        if (consumos.isEmpty()) {
            return semana(consumos, Map.of(), List.of(), Map.of());
        }
        Set<Integer> recetasConsumidas = new HashSet<>(consumos);
        Map<Integer, Integer> comensales = nutricion.comensales(recetasConsumidas).stream()
                .collect(toMap(RecetaComensales::receta, RecetaComensales::comensales));
        return semana(
                consumos,
                comensales,
                nutricion.ingredientes(recetasConsumidas),
                piezasPorIngrediente(recetasConsumidas));
    }

    /** La estimación con los ingredientes de la receta y los pesos de sus piezas (por id de ingrediente). */
    static NutricionRecetaDto calcular(
            int comensales, List<IngredienteNutricion> filas, Map<Integer, Map<String, BigDecimal>> piezas) {
        List<IngredienteNutricionDto> ingredientes = new ArrayList<>();
        Suma suma = sumar(filas, piezas, ingredientes);
        if (suma.vacia()) {
            return new NutricionRecetaDto(comensales, null, null, ingredientes);
        }
        return new NutricionRecetaDto(
                comensales, suma.valores(BigDecimal.ONE), suma.valores(BigDecimal.valueOf(comensales)), ingredientes);
    }

    /**
     * Los nutrientes de la semana: por cada consumo, una ración (el total de su receta entre sus comensales). Los
     * consumos de recetas sin estimación no suman, pero cuentan en {@code consumos}.
     *
     * @param consumos la receta de cada consumo, repetida si se ha comido varias veces
     */
    static NutricionSemanaDto semana(
            List<Integer> consumos,
            Map<Integer, Integer> comensales,
            List<IngredienteNutricion> filas,
            Map<Integer, Map<String, BigDecimal>> piezas) {
        Map<Integer, List<IngredienteNutricion>> porReceta =
                filas.stream().collect(groupingBy(IngredienteNutricion::receta));
        Map<Integer, Suma> recetas = new HashMap<>();
        porReceta.forEach((receta, suyas) -> recetas.put(receta, sumar(suyas, piezas, null)));
        Suma semana = new Suma();
        int estimados = 0;
        for (Integer receta : consumos) {
            Suma suma = recetas.get(receta);
            if (suma != null && !suma.vacia()) {
                semana.anadirRacion(suma, BigDecimal.valueOf(comensales.getOrDefault(receta, 1)));
                estimados++;
            }
        }
        if (semana.vacia()) {
            return new NutricionSemanaDto(consumos.size(), 0, null, null, null);
        }
        BigDecimal dias = BigDecimal.valueOf(RecomendacionService.VENTANA.toDays());
        return new NutricionSemanaDto(
                consumos.size(),
                estimados,
                semana.valores(BigDecimal.ONE),
                semana.valores(dias),
                semana.referencia(dias));
    }

    /**
     * Suma la composición de los ingredientes que cuentan. Si se pasa {@code detalle}, añade a él cómo cuenta cada
     * ingrediente.
     */
    private static Suma sumar(
            List<IngredienteNutricion> filas,
            Map<Integer, Map<String, BigDecimal>> piezas,
            @Nullable List<IngredienteNutricionDto> detalle) {
        Suma suma = new Suma();
        for (IngredienteNutricion fila : filas) {
            EstadoNutricion estado = EstadoNutricion.SIN_DATOS;
            BigDecimal contados = null;
            if (fila.alimento() != null) {
                Optional<BigDecimal> gramos =
                        gramos(fila.cantidad(), piezas.getOrDefault(fila.ingrediente(), Map.of()));
                estado = gramos.isPresent() ? EstadoNutricion.CALCULADO : EstadoNutricion.SIN_PESO;
                if (gramos.isPresent()) {
                    suma.anadir(fila, gramos.get());
                    contados = gramos.get().setScale(1, RoundingMode.HALF_UP);
                }
            }
            if (detalle != null) {
                detalle.add(dto(fila, contados, estado));
            }
        }
        return suma;
    }

    private Map<Integer, Map<String, BigDecimal>> piezasPorIngrediente(Collection<Integer> recetasConsumidas) {
        return nutricion.piezas(recetasConsumidas).stream()
                .collect(groupingBy(PesoPieza::ingrediente, toMap(PesoPieza::pieza, PesoPieza::gramos)));
    }

    /**
     * Los gramos de una cantidad, si se pueden saber: un número seguido de una unidad de {@link #UNIDADES}, de una
     * pieza del ingrediente en singular o en plural («2 dientes») o de nada («2» = 2 unidades). Detrás de la unidad
     * solo se admite «de …» («200 gramos de harina»): «400 gramos cocidos» no se cuenta, porque BEDCA da el alimento
     * crudo.
     */
    static Optional<BigDecimal> gramos(String cantidad, Map<String, BigDecimal> piezas) {
        return CantidadesCompra.leer(cantidad).flatMap(medida -> {
            String[] partes = medida.unidad().split(" ", 2);
            if (partes.length > 1 && !partes[1].startsWith("de ")) {
                return Optional.empty();
            }
            String unidad = partes[0].isEmpty() ? PIEZA_POR_DEFECTO : partes[0];
            BigDecimal porUnidad = UNIDADES.get(unidad);
            if (porUnidad == null) {
                porUnidad = piezas.entrySet().stream()
                        .filter(pieza -> CantidadesCompra.mismaUnidad(pieza.getKey(), unidad))
                        .map(Map.Entry::getValue)
                        .findFirst()
                        .orElse(null);
            }
            return Optional.ofNullable(porUnidad).map(medida.numero()::multiply);
        });
    }

    /** ¿Es una unidad que ya se convierte sola, sin peso de pieza («gramos», «cucharada»)? */
    public static boolean esUnidadGeneral(String palabra) {
        return UNIDADES.containsKey(palabra.strip().toLowerCase(Locale.ROOT));
    }

    /** ¿Son la misma pieza, en singular o en plural y sin tener en cuenta las tildes («lata», «latas»)? */
    public static boolean mismaPieza(String una, String otra) {
        return CantidadesCompra.mismaUnidad(una, otra);
    }

    private static IngredienteNutricionDto dto(
            IngredienteNutricion fila, @Nullable BigDecimal gramos, EstadoNutricion estado) {
        return new IngredienteNutricionDto(
                fila.ingrediente(), fila.nombre(), fila.cantidad(), gramos, fila.alimento(), estado);
    }

    private static Stream<Map.Entry<String, BigDecimal>> unidades(int gramos, String... nombres) {
        return Arrays.stream(nombres).map(nombre -> Map.entry(nombre, BigDecimal.valueOf(gramos)));
    }

    /** Totales sin redondear. Un valor que BEDCA no da ({@code null}) no suma nada. */
    private static final class Suma {
        private boolean vacia = true;
        private BigDecimal energiaKj = BigDecimal.ZERO;
        private BigDecimal grasas = BigDecimal.ZERO;
        private BigDecimal grasasSaturadas = BigDecimal.ZERO;
        private BigDecimal hidratos = BigDecimal.ZERO;
        private BigDecimal fibra = BigDecimal.ZERO;
        private BigDecimal proteinas = BigDecimal.ZERO;
        private BigDecimal sodioMg = BigDecimal.ZERO;

        void anadir(IngredienteNutricion fila, BigDecimal gramos) {
            vacia = false;
            energiaKj = mas(energiaKj, fila.energiaKj(), gramos);
            grasas = mas(grasas, fila.grasas(), gramos);
            grasasSaturadas = mas(grasasSaturadas, fila.grasasSaturadas(), gramos);
            hidratos = mas(hidratos, fila.hidratos(), gramos);
            fibra = mas(fibra, fila.fibra(), gramos);
            proteinas = mas(proteinas, fila.proteinas(), gramos);
            sodioMg = mas(sodioMg, fila.sodioMg(), gramos);
        }

        /** Suma una ración de una receta: su total entre {@code comensales}. */
        void anadirRacion(Suma receta, BigDecimal comensales) {
            vacia = false;
            energiaKj = energiaKj.add(entre(receta.energiaKj, comensales));
            grasas = grasas.add(entre(receta.grasas, comensales));
            grasasSaturadas = grasasSaturadas.add(entre(receta.grasasSaturadas, comensales));
            hidratos = hidratos.add(entre(receta.hidratos, comensales));
            fibra = fibra.add(entre(receta.fibra, comensales));
            proteinas = proteinas.add(entre(receta.proteinas, comensales));
            sodioMg = sodioMg.add(entre(receta.sodioMg, comensales));
        }

        boolean vacia() {
            return vacia;
        }

        /** El total entre {@code divisor}, en porcentaje de la ingesta de referencia, sin decimales. */
        IngestaReferenciaDto referencia(BigDecimal divisor) {
            return new IngestaReferenciaDto(
                    porcentaje(energiaKj, divisor, REFERENCIA_KJ),
                    porcentaje(grasas, divisor, REFERENCIA_GRASAS),
                    porcentaje(grasasSaturadas, divisor, REFERENCIA_SATURADAS),
                    porcentaje(hidratos, divisor, REFERENCIA_HIDRATOS),
                    porcentaje(proteinas, divisor, REFERENCIA_PROTEINAS),
                    porcentaje(
                            sodioMg.multiply(SAL_POR_SODIO).divide(MIL, PRECISION, RoundingMode.HALF_UP),
                            divisor,
                            REFERENCIA_SAL));
        }

        private static int porcentaje(BigDecimal valor, BigDecimal divisor, BigDecimal referencia) {
            return entre(valor, divisor)
                    .multiply(CIEN)
                    .divide(referencia, 0, RoundingMode.HALF_UP)
                    .intValueExact();
        }

        /** Los totales divididos entre {@code divisor} y redondeados. */
        ValoresNutricionalesDto valores(BigDecimal divisor) {
            BigDecimal kj = entre(energiaKj, divisor);
            return new ValoresNutricionalesDto(
                    kj.divide(KJ_POR_KCAL, 0, RoundingMode.HALF_UP),
                    kj.setScale(0, RoundingMode.HALF_UP),
                    entre(grasas, divisor).setScale(1, RoundingMode.HALF_UP),
                    entre(grasasSaturadas, divisor).setScale(1, RoundingMode.HALF_UP),
                    entre(hidratos, divisor).setScale(1, RoundingMode.HALF_UP),
                    entre(fibra, divisor).setScale(1, RoundingMode.HALF_UP),
                    entre(proteinas, divisor).setScale(1, RoundingMode.HALF_UP),
                    entre(sodioMg, divisor).multiply(SAL_POR_SODIO).divide(MIL, 2, RoundingMode.HALF_UP));
        }

        private static BigDecimal mas(BigDecimal total, @Nullable BigDecimal por100g, BigDecimal gramos) {
            return por100g == null
                    ? total
                    : total.add(por100g.multiply(gramos).divide(CIEN, PRECISION, RoundingMode.HALF_UP));
        }

        private static BigDecimal entre(BigDecimal valor, BigDecimal divisor) {
            return valor.divide(divisor, PRECISION, RoundingMode.HALF_UP);
        }
    }
}
