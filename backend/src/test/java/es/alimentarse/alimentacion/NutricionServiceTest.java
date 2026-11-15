package es.alimentarse.alimentacion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import es.alimentarse.receta.RecetaService;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * Estudio nutricional (D-079): conversión de cantidades a gramos y suma de la composición; nutrientes de la semana
 * (D-082).
 */
class NutricionServiceTest {

    private static final Map<String, BigDecimal> SIN_PIEZAS = Map.of();

    // Valores de BEDCA por 100 g (los de la migración V13)
    private static final IngredienteNutricion HARINA =
            fila(1, "Harina", "200 gramos", "Harina de trigo", "1385.5", null, null, "71.5", "3.5", "10", "3");
    private static final IngredienteNutricion HUEVOS = fila(
            2, "Huevos", "2 unidades", "Huevo de gallina fresco", "623.2", "11.1", "3.1", null, "0", "12.5", "140");
    private static final IngredienteNutricion SAL =
            fila(3, "Sal", "1 pizca", "Sal de mar", "0", null, null, "0", "0", "0", "35000");
    private static final IngredienteNutricion LEVADURA =
            new IngredienteNutricion(1, 4, "Levadura", "1 sobre", null, null, null, null, null, null, null, null);
    private static final Map<Integer, Map<String, BigDecimal>> PIEZA_HUEVO =
            Map.of(2, Map.of("unidad", new BigDecimal("50")));
    private static final Instant AHORA = Instant.parse("2026-11-14T12:00:00Z");

    @Test
    void lasUnidadesDeMasaYDeVolumenSeConviertenAGramos() {
        assertThat(gramos("200 gramos")).isEqualByComparingTo("200");
        assertThat(gramos("200 g")).isEqualByComparingTo("200");
        assertThat(gramos("200gr")).isEqualByComparingTo("200");
        assertThat(gramos("1,5 kg")).isEqualByComparingTo("1500");
        assertThat(gramos("1/2 kilo")).isEqualByComparingTo("500");
        assertThat(gramos("500 mililitros")).isEqualByComparingTo("500");
        assertThat(gramos("2 cl")).isEqualByComparingTo("20");
        assertThat(gramos("1 litro")).isEqualByComparingTo("1000");
        assertThat(gramos("2 Cucharadas")).isEqualByComparingTo("30");
        assertThat(gramos("1 cucharadita")).isEqualByComparingTo("5");
    }

    @Test
    void detrasDeLaUnidadSoloSeAdmiteDe() {
        assertThat(gramos("200 gramos de harina")).isEqualByComparingTo("200");
        assertThat(NutricionService.gramos("400 gramos cocidos", SIN_PIEZAS)).isEmpty();
        assertThat(NutricionService.gramos("1 cucharada sopera", SIN_PIEZAS)).isEmpty();
    }

    @Test
    void lasPiezasUsanElPesoDelIngredienteEnSingularOEnPlural() {
        Map<String, BigDecimal> huevo = Map.of("unidad", new BigDecimal("50"));
        Map<String, BigDecimal> ajo = Map.of("diente", new BigDecimal("5"));

        assertThat(NutricionService.gramos("1 unidad", huevo)).contains(new BigDecimal("50"));
        assertThat(NutricionService.gramos("3 unidades", huevo)).contains(new BigDecimal("150"));
        assertThat(NutricionService.gramos("2", huevo)).contains(new BigDecimal("100"));
        assertThat(NutricionService.gramos("2 dientes", ajo)).contains(new BigDecimal("10"));
        assertThat(NutricionService.gramos("1 diente de ajo", ajo)).contains(new BigDecimal("5"));
    }

    @Test
    void losNumerosConLetrasTambienCuentan() {
        Map<String, BigDecimal> cebolla = Map.of("unidad", new BigDecimal("150"));

        assertThat(NutricionService.gramos("media", cebolla))
                .hasValueSatisfying(g -> assertThat(g).isEqualByComparingTo("75"));
        assertThat(NutricionService.gramos("una unidad", cebolla))
                .hasValueSatisfying(g -> assertThat(g).isEqualByComparingTo("150"));
        assertThat(gramos("medio kilo")).isEqualByComparingTo("500");
        assertThat(gramos("una cucharada de aceite")).isEqualByComparingTo("15");
    }

    @Test
    void lasUnidadesGeneralesYLasPiezasIgualesSeReconocen() {
        assertThat(NutricionService.esUnidadGeneral(" Gramos ")).isTrue();
        assertThat(NutricionService.esUnidadGeneral("lata")).isFalse();
        assertThat(NutricionService.mismaPieza("lata", "latas")).isTrue();
        assertThat(NutricionService.mismaPieza("limón", "limones")).isTrue();
        assertThat(NutricionService.mismaPieza("lata", "loncha")).isFalse();
    }

    @Test
    void loQueNoSePuedeConvertirNoTieneGramos() {
        assertThat(NutricionService.gramos("2 unidades", SIN_PIEZAS)).isEmpty();
        assertThat(NutricionService.gramos("2", SIN_PIEZAS)).isEmpty();
        assertThat(NutricionService.gramos("1 pizca", SIN_PIEZAS)).isEmpty();
        assertThat(NutricionService.gramos("al gusto", SIN_PIEZAS)).isEmpty();
        assertThat(NutricionService.gramos("2-3 dientes", Map.of("diente", BigDecimal.ONE)))
                .isEmpty();
    }

    @Test
    void sumaLosIngredientesQueCuentanYDivideEntreLosComensales() {
        NutricionRecetaDto resultado = NutricionService.calcular(
                2, List.of(HARINA, HUEVOS, LEVADURA, SAL), Map.of(2, Map.of("unidad", new BigDecimal("50"))));

        // 200 g de harina + 100 g de huevo; lo que BEDCA no da (null) no suma
        ValoresNutricionalesDto total = resultado.total();
        assertThat(total).isNotNull();
        assertThat(total.energiaKj()).isEqualByComparingTo("3394"); // 2771 + 623,2
        assertThat(total.energiaKcal()).isEqualByComparingTo("811"); // 3394,2 / 4,184
        assertThat(total.grasas()).isEqualByComparingTo("11.1");
        assertThat(total.grasasSaturadas()).isEqualByComparingTo("3.1");
        assertThat(total.hidratos()).isEqualByComparingTo("143");
        assertThat(total.fibra()).isEqualByComparingTo("7");
        assertThat(total.proteinas()).isEqualByComparingTo("32.5");
        assertThat(total.sal()).isEqualByComparingTo("0.37"); // 146 mg de sodio × 2,5

        ValoresNutricionalesDto racion = resultado.porRacion();
        assertThat(racion).isNotNull();
        assertThat(racion.energiaKj()).isEqualByComparingTo("1697");
        assertThat(racion.energiaKcal()).isEqualByComparingTo("406");
        assertThat(racion.grasas()).isEqualByComparingTo("5.6");
        assertThat(racion.proteinas()).isEqualByComparingTo("16.3");
        assertThat(racion.sal()).isEqualByComparingTo("0.18");

        assertThat(resultado.comensales()).isEqualTo(2);
        assertThat(resultado.ingredientes())
                .extracting(IngredienteNutricionDto::nombre, IngredienteNutricionDto::estado)
                .containsExactly(
                        tuple("Harina", EstadoNutricion.CALCULADO),
                        tuple("Huevos", EstadoNutricion.CALCULADO),
                        tuple("Levadura", EstadoNutricion.SIN_DATOS),
                        tuple("Sal", EstadoNutricion.SIN_PESO));
        IngredienteNutricionDto huevos = resultado.ingredientes().get(1);
        assertThat(huevos.gramos()).isEqualByComparingTo("100");
        assertThat(huevos.gramos().scale()).isEqualTo(1);
        assertThat(huevos.alimento()).isEqualTo("Huevo de gallina fresco");
        assertThat(huevos.cantidad()).isEqualTo("2 unidades");
        assertThat(resultado.ingredientes().get(2).alimento()).isNull();
        assertThat(resultado.ingredientes().get(3).gramos()).isNull();
    }

    @Test
    void sinNingunIngredienteQueCuenteNoHayTotales() {
        NutricionRecetaDto resultado = NutricionService.calcular(4, List.of(LEVADURA, SAL), Map.of());

        assertThat(resultado.total()).isNull();
        assertThat(resultado.porRacion()).isNull();
        assertThat(resultado.ingredientes()).hasSize(2);

        assertThat(NutricionService.calcular(4, List.of(), Map.of()).total()).isNull();
    }

    @Test
    void compruebaQueLaRecetaSeVeYAgrupaLasPiezasPorIngrediente() {
        RecetaService recetas = mock(RecetaService.class);
        NutricionMapper mapper = mock(NutricionMapper.class);
        when(mapper.comensales(List.of(7))).thenReturn(List.of(new RecetaComensales(7, 1)));
        when(mapper.ingredientes(List.of(7))).thenReturn(List.of(HUEVOS));
        when(mapper.piezas(List.of(7)))
                .thenReturn(List.of(
                        new PesoPieza(2, "unidad", new BigDecimal("50")),
                        new PesoPieza(9, "loncha", new BigDecimal("15"))));

        NutricionRecetaDto resultado =
                new NutricionService(recetas, mapper, Clock.fixed(AHORA, ZoneOffset.UTC)).deReceta(7, "ana", true);

        verify(recetas).exigirVisible(7, "ana", true);
        assertThat(resultado.total()).isNotNull();
        assertThat(resultado.total().proteinas()).isEqualByComparingTo("12.5");
    }

    @Test
    void laSemanaSumaUnaRacionPorConsumoYDaLaMediaDiariaYLaIngestaDeReferencia() {
        // Dos raciones del bizcocho (1, para 2) y una receta sin estimación (5): la semana es el bizcocho entero
        IngredienteNutricion soloLevadura =
                new IngredienteNutricion(5, 4, "Levadura", "1 sobre", null, null, null, null, null, null, null, null);
        NutricionSemanaDto semana = NutricionService.semana(
                List.of(1, 5, 1), Map.of(1, 2, 5, 4), List.of(HARINA, HUEVOS, LEVADURA, soloLevadura), PIEZA_HUEVO);

        assertThat(semana.consumos()).isEqualTo(3);
        assertThat(semana.estimados()).isEqualTo(2);
        assertThat(semana.total()).isNotNull();
        assertThat(semana.total().energiaKj()).isEqualByComparingTo("3394");
        assertThat(semana.total().energiaKcal()).isEqualByComparingTo("811");
        assertThat(semana.total().proteinas()).isEqualByComparingTo("32.5");
        assertThat(semana.total().sal()).isEqualByComparingTo("0.37");

        // Entre 7 días
        ValoresNutricionalesDto media = semana.mediaDiaria();
        assertThat(media).isNotNull();
        assertThat(media.energiaKj()).isEqualByComparingTo("485");
        assertThat(media.energiaKcal()).isEqualByComparingTo("116");
        assertThat(media.proteinas()).isEqualByComparingTo("4.6");
        assertThat(media.sal()).isEqualByComparingTo("0.05");

        // 485 kJ de 8.400, 1,6 g de grasas de 70, 0,44 g de saturadas de 20, 20,4 g de hidratos de 260,
        // 4,6 g de proteínas de 50 y 0,052 g de sal de 6
        assertThat(semana.referencia()).isEqualTo(new IngestaReferenciaDto(6, 2, 2, 8, 9, 1));
    }

    @Test
    void sinConsumosOSinEstimacionNoHayTotales() {
        assertThat(NutricionService.semana(List.of(), Map.of(), List.of(), Map.of()))
                .isEqualTo(new NutricionSemanaDto(0, 0, null, null, null));
        assertThat(NutricionService.semana(List.of(1, 1), Map.of(1, 2), List.of(LEVADURA, SAL), Map.of()))
                .isEqualTo(new NutricionSemanaDto(2, 0, null, null, null));
    }

    @Test
    void laSemanaEsLaVentanaDeLosIndicadoresYPideCadaRecetaUnaVez() {
        NutricionMapper mapper = mock(NutricionMapper.class);
        NutricionService servicio =
                new NutricionService(mock(RecetaService.class), mapper, Clock.fixed(AHORA, ZoneOffset.UTC));
        when(mapper.recetasConsumidas("ana", AHORA.minus(Duration.ofDays(7)), AHORA))
                .thenReturn(List.of(1, 1));
        when(mapper.comensales(Set.of(1))).thenReturn(List.of(new RecetaComensales(1, 2)));
        when(mapper.ingredientes(Set.of(1))).thenReturn(List.of(HARINA, HUEVOS));
        when(mapper.piezas(Set.of(1))).thenReturn(List.of(new PesoPieza(2, "unidad", new BigDecimal("50"))));

        NutricionSemanaDto semana = servicio.semana("ana");

        assertThat(semana.estimados()).isEqualTo(2);
        assertThat(semana.total().energiaKcal()).isEqualByComparingTo("811");

        when(mapper.recetasConsumidas("luis", AHORA.minus(Duration.ofDays(7)), AHORA))
                .thenReturn(List.of());
        assertThat(servicio.semana("luis").consumos()).isZero();
        verify(mapper, never()).comensales(List.of());
    }

    private static BigDecimal gramos(String cantidad) {
        return NutricionService.gramos(cantidad, SIN_PIEZAS).orElseThrow();
    }

    private static IngredienteNutricion fila(
            int id,
            String nombre,
            String cantidad,
            String alimento,
            String energiaKj,
            String grasas,
            String saturadas,
            String hidratos,
            String fibra,
            String proteinas,
            String sodioMg) {
        return new IngredienteNutricion(
                1,
                id,
                nombre,
                cantidad,
                alimento,
                numero(energiaKj),
                numero(grasas),
                numero(saturadas),
                numero(hidratos),
                numero(fibra),
                numero(proteinas),
                numero(sodioMg));
    }

    private static BigDecimal numero(String valor) {
        return valor == null ? null : new BigDecimal(valor);
    }
}
