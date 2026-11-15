package es.alimentarse.alimentacion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import es.alimentarse.catalogo.CatalogoMapper;
import es.alimentarse.catalogo.GrupoDto;
import es.alimentarse.catalogo.TipoRecetaDto;
import es.alimentarse.comun.Pagina;
import es.alimentarse.receta.FiltroRecetas;
import es.alimentarse.receta.ParametrosFiltroRecetas;
import es.alimentarse.receta.RecetaResumenDto;
import es.alimentarse.receta.RecetaService;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Reglas 6.2, 6.3 y 6.4 de la especificación: límite exacto, grupos sin límite, bordes de la ventana de 7 días,
 * recetas con varios grupos y ausencia de consumos.
 */
class RecomendacionServiceTest {

    private static final Instant AHORA = Instant.parse("2026-11-05T12:00:00Z");
    private static final Instant HACE_7_DIAS = AHORA.minus(Duration.ofDays(7));

    private static final GrupoDto HORTALIZAS = new GrupoDto(4, "Hortalizas", null);
    private static final GrupoDto FRUTA = new GrupoDto(5, "Fruta", null);
    private static final GrupoDto PASTA = new GrupoDto(7, "Pasta", 4);
    private static final GrupoDto PESCADO = new GrupoDto(9, "Pescado", 4);
    private static final GrupoDto CARNE = new GrupoDto(11, "Carne", 3);
    private static final GrupoDto LECHES = new GrupoDto(13, "Leches y derivados", 14);
    private static final GrupoDto DULCES = new GrupoDto(15, "Dulces", 1);
    private static final List<GrupoDto> GRUPOS = List.of(HORTALIZAS, FRUTA, PASTA, PESCADO, CARNE, LECHES, DULCES);

    @Nested
    class RacionesPorGrupo {

        @Test
        void sinConsumosNoHayRaciones() {
            assertThat(RecomendacionService.racionesPorGrupo(List.of(), AHORA)).isEmpty();
        }

        @Test
        void cadaConsumoDistintoCuentaUnaRacion() {
            List<ConsumoGrupo> filas = List.of(
                    fila(1, AHORA.minus(Duration.ofDays(1)), CARNE), fila(2, AHORA.minus(Duration.ofDays(1)), CARNE));

            assertThat(RecomendacionService.racionesPorGrupo(filas, AHORA)).containsExactly(Map.entry(CARNE.id(), 2L));
        }

        @Test
        void unaRecetaConVariosGruposCuentaUnaRacionEnCadaUno() {
            Instant ayer = AHORA.minus(Duration.ofDays(1));
            List<ConsumoGrupo> filas = List.of(fila(1, ayer, CARNE), fila(1, ayer, PASTA), fila(1, ayer, HORTALIZAS));

            assertThat(RecomendacionService.racionesPorGrupo(filas, AHORA))
                    .containsOnly(Map.entry(CARNE.id(), 1L), Map.entry(PASTA.id(), 1L), Map.entry(HORTALIZAS.id(), 1L));
        }

        @Test
        void dosIngredientesDelMismoGrupoEnUnaRecetaCuentanUnaSolaRacion() {
            Instant ayer = AHORA.minus(Duration.ofDays(1));
            List<ConsumoGrupo> filas = List.of(fila(1, ayer, CARNE), fila(1, ayer, CARNE));

            assertThat(RecomendacionService.racionesPorGrupo(filas, AHORA)).containsExactly(Map.entry(CARNE.id(), 1L));
        }

        @Test
        void elBordeInferiorDeLaVentanaCuentaYUnSegundoAntesNo() {
            List<ConsumoGrupo> filas =
                    List.of(fila(1, HACE_7_DIAS, CARNE), fila(2, HACE_7_DIAS.minusSeconds(1), PASTA));

            assertThat(RecomendacionService.racionesPorGrupo(filas, AHORA)).containsExactly(Map.entry(CARNE.id(), 1L));
        }

        @Test
        void elBordeSuperiorDeLaVentanaCuentaYUnConsumoFuturoNo() {
            List<ConsumoGrupo> filas = List.of(fila(1, AHORA, CARNE), fila(2, AHORA.plusSeconds(1), PASTA));

            assertThat(RecomendacionService.racionesPorGrupo(filas, AHORA)).containsExactly(Map.entry(CARNE.id(), 1L));
        }
    }

    @Nested
    class GruposAgotados {

        @Test
        void sinConsumosNingunGrupoEstaAgotado() {
            assertThat(RecomendacionService.gruposAgotados(Map.of(), GRUPOS)).isEmpty();
        }

        @Test
        void unaSolaRecetaConDulcesAgotaElGrupoDeUnaRacionSemanal() {
            assertThat(RecomendacionService.gruposAgotados(Map.of(DULCES.id(), 1L), GRUPOS))
                    .containsExactly(DULCES.id());
        }

        @ParameterizedTest(name = "Carne con {0} de 3 raciones: agotado = {1}")
        @CsvSource({"1, false", "2, false", "3, true", "4, true"})
        void elGrupoSeAgotaAlLlegarExactamenteAlLimite(long consumidas, boolean agotado) {
            assertThat(RecomendacionService.gruposAgotados(Map.of(CARNE.id(), consumidas), GRUPOS)
                            .contains(CARNE.id()))
                    .isEqualTo(agotado);
        }

        @Test
        void unGrupoSinLimiteNuncaSeAgota() {
            assertThat(RecomendacionService.gruposAgotados(Map.of(FRUTA.id(), 50L, HORTALIZAS.id(), 1000L), GRUPOS))
                    .isEmpty();
        }

        @Test
        void unaRecetaConVariosGruposPuedeAgotarVariosALaVez() {
            Map<Integer, Long> raciones = Map.of(CARNE.id(), 3L, PASTA.id(), 4L, PESCADO.id(), 3L);

            assertThat(RecomendacionService.gruposAgotados(raciones, GRUPOS))
                    .containsExactlyInAnyOrder(CARNE.id(), PASTA.id());
        }
    }

    @Nested
    class Indicadores {

        @Test
        void sinConsumosNoHayIndicadores() {
            assertThat(RecomendacionService.indicadores(Map.of(), GRUPOS)).isEmpty();
        }

        @Test
        void elEjemploDeLaMemoriaYSuOrden() {
            Map<Integer, Long> raciones = Map.of(CARNE.id(), 3L, PASTA.id(), 3L, PESCADO.id(), 1L, FRUTA.id(), 3L);

            assertThat(RecomendacionService.indicadores(raciones, GRUPOS))
                    .containsExactly(
                            new IndicadorGrupoDto(CARNE, 3, 3, 100, ColorIndicador.ROJO),
                            new IndicadorGrupoDto(PASTA, 3, 4, 75, ColorIndicador.AMARILLO),
                            new IndicadorGrupoDto(PESCADO, 1, 4, 25, ColorIndicador.VERDE),
                            new IndicadorGrupoDto(FRUTA, 3, null, 100, ColorIndicador.VERDE));
        }

        @ParameterizedTest(name = "{0}/{1} → {2} % {3}")
        @CsvSource({
            "2, 3, 67, VERDE",
            "10, 14, 71, VERDE",
            "3, 4, 75, AMARILLO",
            "11, 14, 79, AMARILLO",
            "13, 14, 93, AMARILLO",
            "14, 14, 100, ROJO",
            "5, 4, 100, ROJO"
        })
        void elColorYElPorcentajeDependenDeLaProporcion(
                long consumidas, int limite, int porcentaje, ColorIndicador color) {
            GrupoDto grupo = new GrupoDto(99, "Grupo", limite);

            assertThat(RecomendacionService.indicador(grupo, consumidas))
                    .isEqualTo(new IndicadorGrupoDto(grupo, consumidas, limite, porcentaje, color));
        }

        @Test
        void losGruposSinLimiteVanAlFinalOrdenadosPorNombre() {
            Map<Integer, Long> raciones = Map.of(HORTALIZAS.id(), 9L, FRUTA.id(), 1L, LECHES.id(), 1L);

            assertThat(RecomendacionService.indicadores(raciones, GRUPOS))
                    .extracting(indicador -> indicador.grupo().nombre())
                    .containsExactly("Leches y derivados", "Fruta", "Hortalizas");
        }
    }

    /** Variedad y explicación de la recomendación, y aviso previo a consumir (D-067). */
    @Nested
    class VariedadYAviso {

        @Test
        void losConsumidosSonLosGruposConLimiteDeLosQueSeHaComidoAlgo() {
            Map<Integer, Long> raciones = Map.of(CARNE.id(), 1L, FRUTA.id(), 5L, PASTA.id(), 0L);

            assertThat(RecomendacionService.gruposConsumidos(raciones, GRUPOS)).containsExactly(CARNE.id());
        }

        @Test
        void cercaDelLimiteEstanLosIndicadoresEnAmbarOEnRojo() {
            Map<Integer, Long> raciones = Map.of(
                    PASTA.id(), 3L, // 3/4: ámbar
                    CARNE.id(), 3L, // 3/3: rojo
                    PESCADO.id(), 2L, // 2/4: verde
                    FRUTA.id(), 50L); // sin límite: verde

            assertThat(RecomendacionService.gruposCercaDelLimite(raciones, GRUPOS))
                    .isEqualTo(Set.of(PASTA.id(), CARNE.id()));
        }

        @Test
        void sinConsumosNadaPenalizaALasRecetas() {
            assertThat(RecomendacionService.gruposConsumidos(Map.of(), GRUPOS)).isEmpty();
            assertThat(RecomendacionService.gruposCercaDelLimite(Map.of(), GRUPOS))
                    .isEmpty();
        }

        @Test
        void elEstadoDeUnaRecetaIncluyeLosGruposAunSinComer() {
            List<GrupoDeReceta> filas = List.of(fila(HORTALIZAS, false), fila(PASTA, false), fila(CARNE, false));

            assertThat(RecomendacionService.estadoDeSusGrupos(filas, Map.of(CARNE.id(), 2L)))
                    .containsExactly(
                            new IndicadorGrupoDto(CARNE, 2, 3, 67, ColorIndicador.VERDE),
                            new IndicadorGrupoDto(PASTA, 0, 4, 0, ColorIndicador.VERDE),
                            new IndicadorGrupoDto(HORTALIZAS, 0, null, 100, ColorIndicador.VERDE));
        }

        @Test
        void losGruposBasicosSinLimiteNoExplicanNada() {
            GrupoDto especias = new GrupoDto(2, "Especias", null);
            GrupoDto basicoConLimite = new GrupoDto(3, "Básico limitado", 2);

            assertThat(RecomendacionService.estadoDeSusGrupos(
                            List.of(fila(especias, true), fila(basicoConLimite, true)), Map.of()))
                    .extracting(IndicadorGrupoDto::grupo)
                    .containsExactly(basicoConLimite);
        }

        @Test
        void elImpactoSumaUnaRacionALosGruposConLimite() {
            List<GrupoDeReceta> filas = List.of(fila(HORTALIZAS, false), fila(PESCADO, false));

            assertThat(RecomendacionService.impacto(filas, Map.of(PESCADO.id(), 1L, HORTALIZAS.id(), 9L)))
                    .containsExactly(new ImpactoGrupoDto(
                            new IndicadorGrupoDto(PESCADO, 1, 4, 25, ColorIndicador.VERDE),
                            new IndicadorGrupoDto(PESCADO, 2, 4, 50, ColorIndicador.VERDE)));
        }

        @Test
        void elImpactoAvisaDeLoQueSeLlenaAntes() {
            List<GrupoDeReceta> filas = List.of(fila(PESCADO, false), fila(CARNE, false));

            assertThat(RecomendacionService.impacto(filas, Map.of(CARNE.id(), 2L)))
                    .extracting(
                            i -> i.despues().grupo().nombre(), i -> i.despues().color())
                    .containsExactly(
                            org.assertj.core.groups.Tuple.tuple("Carne", ColorIndicador.ROJO),
                            org.assertj.core.groups.Tuple.tuple("Pescado", ColorIndicador.VERDE));
        }

        @Test
        void elImpactoPuedePasarseDelLimite() {
            assertThat(RecomendacionService.impacto(List.of(fila(DULCES, false)), Map.of(DULCES.id(), 1L)))
                    .singleElement()
                    .satisfies(i -> {
                        assertThat(i.antes().consumidas()).isEqualTo(1);
                        assertThat(i.despues().consumidas()).isEqualTo(2);
                        assertThat(i.despues().porcentaje()).isEqualTo(100);
                    });
        }

        @Test
        void soloHayExcesoAlSuperarElLimiteEnLosGruposDeLaComida() {
            List<ConsumoGrupo> filas = List.of(
                    new ConsumoGrupo(1, HACE_7_DIAS.plusSeconds(1), DULCES.id()),
                    new ConsumoGrupo(2, AHORA.minusSeconds(60), DULCES.id()),
                    new ConsumoGrupo(3, AHORA.minusSeconds(60), PASTA.id()),
                    new ConsumoGrupo(4, AHORA.minusSeconds(60), FRUTA.id()),
                    new ConsumoGrupo(5, AHORA.minusSeconds(30), FRUTA.id()));

            assertThat(RecomendacionService.excesos(filas, AHORA, List.of(DULCES.id(), PASTA.id(), FRUTA.id()), GRUPOS))
                    .containsExactly(new IndicadorGrupoDto(DULCES, 2, 1, 100, ColorIndicador.ROJO));
            assertThat(RecomendacionService.excesos(filas, AHORA, List.of(PASTA.id()), GRUPOS))
                    .isEmpty();
        }

        @Test
        void unaRecetaSinGruposConLimiteNoTieneImpacto() {
            assertThat(RecomendacionService.impacto(List.of(fila(FRUTA, false)), Map.of()))
                    .isEmpty();
        }

        private static GrupoDeReceta fila(GrupoDto grupo, boolean basico) {
            return new GrupoDeReceta(1, grupo.id(), grupo.nombre(), grupo.racionesSemana(), basico);
        }
    }

    @Nested
    @ExtendWith(MockitoExtension.class)
    class ConLaBaseDeDatos {

        @Mock
        AlimentacionMapper alimentacion;

        @Mock
        CatalogoMapper catalogo;

        @Mock
        RecetaService recetas;

        @Test
        void laRecomendacionExcluyeLosAgotadosYPideOrdenarPorVariedad() {
            RecomendacionService servicio = servicio();
            when(catalogo.grupos()).thenReturn(GRUPOS);
            when(alimentacion.consumosConGrupos("lucia", HACE_7_DIAS, AHORA))
                    .thenReturn(List.of(
                            fila(1, AHORA.minus(Duration.ofHours(5)), DULCES),
                            fila(1, AHORA.minus(Duration.ofHours(5)), LECHES)));
            Pagina<RecetaResumenDto> vacia = Pagina.de(List.of(), 0, 9, 0);
            when(recetas.buscarPaginaPorVariedad(
                            org.mockito.ArgumentMatchers.any(),
                            eq("lucia"),
                            org.mockito.ArgumentMatchers.any(),
                            org.mockito.ArgumentMatchers.any(),
                            eq(0),
                            anyInt()))
                    .thenReturn(vacia);

            FiltroRecetas filtro =
                    FiltroRecetas.de(new ParametrosFiltroRecetas("tarta", null, null, null, null, null, null));
            assertThat(servicio.recomendadas(filtro, "lucia", 0, 9).contenido()).isEmpty();

            ArgumentCaptor<FiltroRecetas> usado = ArgumentCaptor.forClass(FiltroRecetas.class);
            verify(recetas)
                    .buscarPaginaPorVariedad(
                            usado.capture(),
                            eq("lucia"),
                            eq(Set.of(DULCES.id(), LECHES.id())),
                            eq(Set.of(DULCES.id())),
                            eq(0),
                            eq(9));
            assertThat(usado.getValue().soloPublicadas()).isTrue();
            assertThat(usado.getValue().gruposExcluidos()).containsExactly(DULCES.id());
            assertThat(usado.getValue().titulo()).isEqualTo("%tarta%");
        }

        @Test
        void cadaRecetaRecomendadaLlevaElEstadoDeSusGrupos() {
            RecomendacionService servicio = servicio();
            when(catalogo.grupos()).thenReturn(GRUPOS);
            when(alimentacion.consumosConGrupos("lucia", HACE_7_DIAS, AHORA))
                    .thenReturn(List.of(fila(1, AHORA.minus(Duration.ofDays(1)), CARNE)));
            RecetaResumenDto receta = new RecetaResumenDto(
                    7,
                    "Pollo",
                    "marcos",
                    new TipoRecetaDto(1, "Plato"),
                    null,
                    20,
                    2,
                    "Fácil",
                    0,
                    false,
                    null,
                    0,
                    true,
                    AHORA,
                    false,
                    false);
            when(recetas.buscarPaginaPorVariedad(
                            org.mockito.ArgumentMatchers.any(),
                            eq("lucia"),
                            org.mockito.ArgumentMatchers.any(),
                            org.mockito.ArgumentMatchers.any(),
                            eq(0),
                            eq(9)))
                    .thenReturn(Pagina.de(List.of(receta), 0, 9, 1));
            when(alimentacion.gruposDeRecetas(List.of(7)))
                    .thenReturn(List.of(new GrupoDeReceta(7, CARNE.id(), "Carne", 3, false)));

            FiltroRecetas filtro =
                    FiltroRecetas.de(new ParametrosFiltroRecetas(null, null, null, null, null, null, null));
            Pagina<RecetaRecomendadaDto> pagina = servicio.recomendadas(filtro, "lucia", 0, 9);

            assertThat(pagina.totalElementos()).isEqualTo(1);
            assertThat(pagina.contenido())
                    .containsExactly(new RecetaRecomendadaDto(
                            receta, List.of(new IndicadorGrupoDto(CARNE, 1, 3, 33, ColorIndicador.VERDE))));
        }

        @Test
        void elImpactoSoloSeCalculaParaRecetasVisibles() {
            RecomendacionService servicio = servicio();
            org.mockito.Mockito.doThrow(es.alimentarse.comun.error.ExcepcionApi.noEncontrado("No"))
                    .when(recetas)
                    .exigirVisible(7, "lucia");

            org.assertj.core.api.Assertions.assertThatThrownBy(() -> servicio.impacto(7, "lucia"))
                    .isInstanceOf(es.alimentarse.comun.error.ExcepcionApi.class);
            org.mockito.Mockito.verifyNoInteractions(alimentacion);
        }

        @Test
        void losIndicadoresUsanLaVentanaDeLosUltimos7Dias() {
            RecomendacionService servicio = servicio();
            when(catalogo.grupos()).thenReturn(GRUPOS);
            List<ConsumoGrupo> filas = new ArrayList<>();
            filas.add(fila(1, AHORA.minus(Duration.ofDays(2)), CARNE));
            when(alimentacion.consumosConGrupos("lucia", HACE_7_DIAS, AHORA)).thenReturn(filas);

            assertThat(servicio.indicadores("lucia"))
                    .containsExactly(new IndicadorGrupoDto(CARNE, 1, 3, 33, ColorIndicador.VERDE));
        }

        private RecomendacionService servicio() {
            return new RecomendacionService(alimentacion, catalogo, recetas, Clock.fixed(AHORA, ZoneOffset.UTC));
        }
    }

    private static ConsumoGrupo fila(long consumo, Instant fecha, GrupoDto grupo) {
        return new ConsumoGrupo(consumo, fecha, grupo.id());
    }
}
