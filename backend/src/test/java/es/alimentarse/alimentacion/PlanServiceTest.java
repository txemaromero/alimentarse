package es.alimentarse.alimentacion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import es.alimentarse.catalogo.CatalogoMapper;
import es.alimentarse.catalogo.GrupoDto;
import es.alimentarse.catalogo.TipoRecetaDto;
import es.alimentarse.comun.error.ExcepcionApi;
import es.alimentarse.consumo.ConsumoDto;
import es.alimentarse.consumo.ConsumoRequest;
import es.alimentarse.consumo.ConsumoService;
import es.alimentarse.receta.RecetaResumenDto;
import es.alimentarse.receta.RecetaService;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

@ExtendWith(MockitoExtension.class)
class PlanServiceTest {

    private static final Instant AHORA = Instant.parse("2026-11-05T12:00:00Z");
    private static final Instant DESDE = AHORA.minus(Duration.ofHours(12));
    private static final Instant HASTA = DESDE.plus(Duration.ofDays(7)).minusSeconds(1);
    private static final GrupoDto CARNE = new GrupoDto(11, "Carne", 3);
    private static final GrupoDto DULCES = new GrupoDto(15, "Dulces", 1);
    private static final GrupoDto FRUTA = new GrupoDto(5, "Fruta", null);
    private static final int POLLO = 1;
    private static final int TARTA = 2;

    @Mock
    PlanMapper plan;

    @Mock
    AlimentacionMapper alimentacion;

    @Mock
    CatalogoMapper catalogo;

    @Mock
    RecetaService recetas;

    @Mock
    ConsumoService consumos;

    private PlanService servicio;

    @BeforeEach
    void crearServicio() {
        servicio = new PlanService(plan, alimentacion, catalogo, recetas, consumos, Clock.fixed(AHORA, ZoneOffset.UTC));
        lenient().when(catalogo.grupos()).thenReturn(List.of(CARNE, DULCES, FRUTA));
        lenient()
                .when(alimentacion.gruposDeRecetas(any()))
                .thenReturn(List.of(
                        new GrupoDeReceta(POLLO, CARNE.id(), "Carne", 3, false),
                        new GrupoDeReceta(TARTA, DULCES.id(), "Dulces", 1, false)));
    }

    @Test
    void elIntervaloDebeSerValidoYNoDemasiadoLargo() {
        assertThatThrownBy(() -> servicio.plan(HASTA, DESDE, "lucia"))
                .isInstanceOfSatisfying(
                        ExcepcionApi.class, e -> assertThat(e.getEstado()).isEqualTo(HttpStatus.BAD_REQUEST));
        assertThatThrownBy(() -> servicio.plan(DESDE, DESDE, "lucia")).isInstanceOf(ExcepcionApi.class);
        assertThatThrownBy(() -> servicio.plan(DESDE, DESDE.plus(Duration.ofDays(32)), "lucia"))
                .isInstanceOf(ExcepcionApi.class);
        verifyNoInteractions(plan);
    }

    @Test
    void sinComidasLosIndicadoresSonLosDeLoComidoDentroDeLaVentana() {
        consumosReales(new ConsumoGrupo(1, AHORA.minus(Duration.ofHours(2)), CARNE.id()));
        comidas();

        PlanDto resultado = servicio.plan(DESDE, HASTA, "lucia");

        assertThat(resultado.comidas()).isEmpty();
        assertThat(resultado.indicadores())
                .containsExactly(new IndicadorGrupoDto(CARNE, 1, 3, 33, ColorIndicador.VERDE));
    }

    @Test
    void loPlanificadoSumaALoComido() {
        consumosReales(new ConsumoGrupo(1, AHORA.minus(Duration.ofHours(2)), CARNE.id()));
        comidas(comida(7, POLLO, AHORA.plus(Duration.ofDays(1))));
        tarjetas(tarjeta(POLLO, true, false, false, "marcos"));

        PlanDto resultado = servicio.plan(DESDE, HASTA, "lucia");

        assertThat(resultado.indicadores())
                .containsExactly(new IndicadorGrupoDto(CARNE, 2, 3, 67, ColorIndicador.VERDE));
        assertThat(resultado.comidas()).singleElement().satisfies(c -> {
            assertThat(c.id()).isEqualTo(7);
            assertThat(c.disponible()).isTrue();
            assertThat(c.excesos()).isEmpty();
        });
    }

    @Test
    void unaComidaQueYaHaPasadoNoCuentaSiNoSeHaRegistrado() {
        consumosReales();
        comidas(comida(7, POLLO, AHORA.minus(Duration.ofHours(1))));
        tarjetas(tarjeta(POLLO, true, false, false, "marcos"));

        PlanDto resultado = servicio.plan(DESDE, HASTA, "lucia");

        assertThat(resultado.indicadores()).isEmpty();
        assertThat(resultado.comidas()).hasSize(1);
    }

    @Test
    void lasRecetasQueYaNoSePuedenVerNoCuentanYSeAvisa() {
        consumosReales();
        comidas(comida(7, POLLO, AHORA.plus(Duration.ofDays(1))), comida(8, TARTA, AHORA.plus(Duration.ofDays(2))));
        tarjetas(tarjeta(POLLO, false, false, false, "marcos"), tarjeta(TARTA, true, true, false, "marcos"));

        PlanDto resultado = servicio.plan(DESDE, HASTA, "lucia");

        assertThat(resultado.indicadores()).isEmpty();
        assertThat(resultado.comidas())
                .extracting(ComidaPlanificadaDto::disponible)
                .containsExactly(false, false);
    }

    @Test
    void unaRecetaEliminadaNoEstaDisponibleNiParaSuAutor() {
        consumosReales();
        comidas(comida(7, POLLO, AHORA.plus(Duration.ofDays(1))));
        tarjetas(tarjeta(POLLO, false, false, true, "lucia"));

        assertThat(servicio.plan(DESDE, HASTA, "lucia").comidas())
                .extracting(ComidaPlanificadaDto::disponible)
                .containsExactly(false);
    }

    @Test
    void elAutorPuedePlanificarSuRecetaSinPublicar() {
        consumosReales();
        comidas(comida(7, POLLO, AHORA.plus(Duration.ofDays(1))));
        tarjetas(tarjeta(POLLO, false, false, false, "lucia"));

        PlanDto resultado = servicio.plan(DESDE, HASTA, "lucia");

        assertThat(resultado.comidas())
                .extracting(ComidaPlanificadaDto::disponible)
                .containsExactly(true);
        assertThat(resultado.indicadores()).hasSize(1);
    }

    @Test
    void avisaSiLaComidaSuperaraElLimiteConLoComido() {
        consumosReales(
                new ConsumoGrupo(1, AHORA.minus(Duration.ofDays(1)), CARNE.id()),
                new ConsumoGrupo(2, AHORA.minus(Duration.ofDays(2)), CARNE.id()),
                new ConsumoGrupo(3, AHORA.minus(Duration.ofDays(3)), CARNE.id()));
        comidas(comida(7, POLLO, AHORA.plus(Duration.ofHours(6))));
        tarjetas(tarjeta(POLLO, true, false, false, "marcos"));

        assertThat(servicio.plan(DESDE, HASTA, "lucia").comidas().getFirst().excesos())
                .containsExactly(new IndicadorGrupoDto(CARNE, 4, 3, 100, ColorIndicador.ROJO));
    }

    @Test
    void llegarJustoAlLimiteNoEsUnExceso() {
        consumosReales(
                new ConsumoGrupo(1, AHORA.minus(Duration.ofDays(1)), CARNE.id()),
                new ConsumoGrupo(2, AHORA.minus(Duration.ofDays(2)), CARNE.id()));
        comidas(comida(7, POLLO, AHORA.plus(Duration.ofHours(6))));
        tarjetas(tarjeta(POLLO, true, false, false, "marcos"));

        assertThat(servicio.plan(DESDE, HASTA, "lucia").comidas().getFirst().excesos())
                .isEmpty();
    }

    @Test
    void loComidoHaceMasDe7DiasRespectoALaComidaYaNoCuenta() {
        consumosReales(new ConsumoGrupo(1, AHORA.minus(Duration.ofDays(6)), DULCES.id()));
        comidas(comida(7, TARTA, AHORA.plus(Duration.ofDays(2))));
        tarjetas(tarjeta(TARTA, true, false, false, "marcos"));

        assertThat(servicio.plan(DESDE, HASTA, "lucia").comidas().getFirst().excesos())
                .isEmpty();
    }

    @Test
    void unaComidaPlanificadaCuentaParaLasSiguientes() {
        consumosReales();
        comidas(comida(7, TARTA, AHORA.plus(Duration.ofDays(1))), comida(8, TARTA, AHORA.plus(Duration.ofDays(2))));
        tarjetas(tarjeta(TARTA, true, false, false, "marcos"));

        List<ComidaPlanificadaDto> resultado =
                servicio.plan(DESDE, HASTA, "lucia").comidas();

        assertThat(resultado.get(0).excesos()).isEmpty();
        assertThat(resultado.get(1).excesos())
                .containsExactly(new IndicadorGrupoDto(DULCES, 2, 1, 100, ColorIndicador.ROJO));
    }

    @Test
    void lasComidasAnterioresAlIntervaloCuentanPeroNoSeDevuelven() {
        Instant semanaQueViene = DESDE.plus(Duration.ofDays(7));
        consumosReales();
        comidas(
                comida(7, TARTA, semanaQueViene.minus(Duration.ofDays(1))),
                comida(8, TARTA, semanaQueViene.plus(Duration.ofDays(1))));
        tarjetas(tarjeta(TARTA, true, false, false, "marcos"));

        assertThat(servicio.plan(semanaQueViene, semanaQueViene.plus(Duration.ofDays(7)), "lucia")
                        .comidas())
                .singleElement()
                .satisfies(c -> {
                    assertThat(c.id()).isEqualTo(8);
                    assertThat(c.excesos()).hasSize(1);
                });
    }

    @Test
    void anadeLaComidaSinFraccionesDeSegundo() {
        Instant fecha = AHORA.plus(Duration.ofDays(1)).plusMillis(250);
        Instant truncada = AHORA.plus(Duration.ofDays(1));
        consumosReales();
        comidas(comida(7, POLLO, truncada));
        tarjetas(tarjeta(POLLO, true, false, false, "marcos"));

        ComidaPlanificadaDto creada = servicio.anadir(new ComidaPlanRequest(POLLO, fecha), "lucia");

        verify(plan).insertar("lucia", POLLO, truncada);
        assertThat(creada.id()).isEqualTo(7);
    }

    @Test
    void noSePuedePlanificarUnaRecetaQueNoSeVe() {
        doThrow(ExcepcionApi.noEncontrado("No existe")).when(recetas).exigirVisible(POLLO, "lucia");

        assertThatThrownBy(() -> servicio.anadir(new ComidaPlanRequest(POLLO, AHORA.plusSeconds(3600)), "lucia"))
                .isInstanceOfSatisfying(
                        ExcepcionApi.class, e -> assertThat(e.getEstado()).isEqualTo(HttpStatus.NOT_FOUND));
        verify(plan, never()).insertar(any(), anyInt(), any());
    }

    @Test
    void noSePuedePlanificarElPasadoPeroSeToleraElDesfaseDelReloj() {
        assertThatThrownBy(() ->
                        servicio.anadir(new ComidaPlanRequest(POLLO, AHORA.minus(Duration.ofMinutes(2))), "lucia"))
                .isInstanceOfSatisfying(
                        ExcepcionApi.class,
                        e -> assertThat(e.getErrores()).containsEntry("fecha", PlanService.FECHA_PASADA));

        Instant casiAhora = AHORA.minusSeconds(30);
        consumosReales();
        comidas(comida(7, POLLO, casiAhora));
        tarjetas(tarjeta(POLLO, true, false, false, "marcos"));
        servicio.anadir(new ComidaPlanRequest(POLLO, casiAhora), "lucia");
        verify(plan).insertar("lucia", POLLO, casiAhora);
    }

    @Test
    void noSePuedePlanificarMasAllaDelHorizonte() {
        assertThatThrownBy(() -> servicio.anadir(
                        new ComidaPlanRequest(
                                POLLO, AHORA.plus(PlanService.HORIZONTE).plusSeconds(1)),
                        "lucia"))
                .isInstanceOfSatisfying(
                        ExcepcionApi.class,
                        e -> assertThat(e.getErrores()).containsEntry("fecha", PlanService.FECHA_LEJANA));
    }

    @Test
    void unHuecoSoloAdmiteUnaReceta() {
        Instant fecha = AHORA.plus(Duration.ofDays(1));
        when(plan.existeHueco("lucia", fecha)).thenReturn(true);

        assertThatThrownBy(() -> servicio.anadir(new ComidaPlanRequest(POLLO, fecha), "lucia"))
                .isInstanceOfSatisfying(ExcepcionApi.class, e -> {
                    assertThat(e.getEstado()).isEqualTo(HttpStatus.CONFLICT);
                    assertThat(e.getCodigo()).isEqualTo("HUECO_OCUPADO");
                });
        verify(plan, never()).insertar(any(), anyInt(), any());
    }

    @Test
    void comoMucho200ComidasPendientes() {
        // D-089: las ya pasadas no cuentan, solo las que aún no han llegado
        Instant fecha = AHORA.plus(Duration.ofDays(1));
        when(plan.contarPendientes("lucia", AHORA)).thenReturn(PlanService.MAXIMO_PENDIENTES);

        assertThatThrownBy(() -> servicio.anadir(new ComidaPlanRequest(POLLO, fecha), "lucia"))
                .isInstanceOfSatisfying(ExcepcionApi.class, e -> {
                    assertThat(e.getEstado()).isEqualTo(HttpStatus.CONFLICT);
                    assertThat(e.getCodigo()).isEqualTo("LIMITE_PLANIFICADAS");
                    assertThat(e.getMessage()).isEqualTo(PlanService.DEMASIADAS_PENDIENTES);
                });
        verify(plan, never()).insertar(any(), anyInt(), any());
    }

    @Test
    void borraSoloLasPropias() {
        when(plan.bloquear(7)).thenReturn(Optional.of(new ComidaPlanificada(7, "lucia", POLLO, AHORA)));
        when(plan.bloquear(8)).thenReturn(Optional.of(new ComidaPlanificada(8, "marcos", POLLO, AHORA)));

        servicio.borrar(7, "lucia");

        verify(plan).borrar(7);
        assertThatThrownBy(() -> servicio.borrar(8, "lucia"))
                .isInstanceOfSatisfying(
                        ExcepcionApi.class, e -> assertThat(e.getEstado()).isEqualTo(HttpStatus.NOT_FOUND));
        verify(plan, never()).borrar(8);
    }

    @Test
    void unaComidaInexistenteEsUn404() {
        when(plan.bloquear(9)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.consumir(9, "lucia"))
                .isInstanceOfSatisfying(
                        ExcepcionApi.class, e -> assertThat(e.getEstado()).isEqualTo(HttpStatus.NOT_FOUND));
    }

    @Test
    void alConsumirUnaComidaFuturaElConsumoEsDeAhora() {
        Instant futura = AHORA.plus(Duration.ofHours(5));
        when(plan.bloquear(7)).thenReturn(Optional.of(new ComidaPlanificada(7, "lucia", POLLO, futura)));
        ConsumoDto consumo = new ConsumoDto(3, AHORA, null);
        when(consumos.registrar(new ConsumoRequest(POLLO, AHORA), "lucia")).thenReturn(consumo);

        assertThat(servicio.consumir(7, "lucia")).isSameAs(consumo);

        InOrder orden = inOrder(plan, consumos);
        orden.verify(plan).bloquear(7);
        orden.verify(consumos).registrar(new ConsumoRequest(POLLO, AHORA), "lucia");
        orden.verify(plan).borrar(7);
    }

    @Test
    void alConsumirUnaComidaYaPasadaElConsumoEsDeSuMomento() {
        Instant pasada = AHORA.minus(Duration.ofHours(3));
        when(plan.bloquear(7)).thenReturn(Optional.of(new ComidaPlanificada(7, "lucia", POLLO, pasada)));
        when(consumos.registrar(new ConsumoRequest(POLLO, pasada), "lucia"))
                .thenReturn(new ConsumoDto(3, pasada, null));

        servicio.consumir(7, "lucia");

        verify(plan).borrar(7);
    }

    @Test
    void siNoSePuedeConsumirLaComidaSigueEnElPlan() {
        when(plan.bloquear(7)).thenReturn(Optional.of(new ComidaPlanificada(7, "lucia", POLLO, AHORA)));
        when(consumos.registrar(any(), eq("lucia"))).thenThrow(ExcepcionApi.noEncontrado("La receta no existe."));

        assertThatThrownBy(() -> servicio.consumir(7, "lucia")).isInstanceOf(ExcepcionApi.class);

        verify(plan, never()).borrar(anyLong());
    }

    @Test
    void laListaDeLaCompraExigeUnIntervaloValido() {
        assertThatThrownBy(() -> servicio.listaCompra(HASTA, DESDE, "lucia"))
                .isInstanceOfSatisfying(
                        ExcepcionApi.class, e -> assertThat(e.getEstado()).isEqualTo(HttpStatus.BAD_REQUEST));
        verifyNoInteractions(plan);
    }

    @Test
    void laListaDeLaCompraSoloLlevaLoQueAunNoHaPasadoYTieneLaRecetaDisponible() {
        Instant futura = AHORA.plus(Duration.ofHours(2));
        comidas(
                comida(1, POLLO, AHORA.minus(Duration.ofHours(2))),
                comida(2, POLLO, futura),
                comida(3, TARTA, futura.plus(Duration.ofDays(1))),
                comida(4, 99, futura.plus(Duration.ofDays(2))));
        // La tarta está oculta y la receta 99 ya no existe
        tarjetas(tarjeta(POLLO, true, false, false, "autor"), tarjeta(TARTA, true, true, false, "autor"));
        when(plan.ingredientesDeRecetas(Set.of(POLLO)))
                .thenReturn(List.of(ingrediente(POLLO, 10, "Pollo", "1 unidad", CARNE, false)));

        ListaCompraDto lista = servicio.listaCompra(DESDE, HASTA, "lucia");

        // Desde ahora, no desde el principio del intervalo, que ya ha pasado
        verify(plan).comidas("lucia", AHORA, HASTA);
        assertThat(lista.comidas()).containsExactly(new ComidaCompraDto(2, futura, POLLO, "Receta 1"));
        assertThat(lista.noDisponibles()).isEqualTo(2);
        assertThat(lista.secciones()).singleElement().satisfies(seccion -> {
            assertThat(seccion.grupo()).isEqualTo(CARNE);
            assertThat(seccion.articulos())
                    .containsExactly(new ArticuloCompraDto(
                            10, "Pollo", "1 unidad", List.of(new CantidadCompraDto(POLLO, "Receta 1", "1 unidad", 1))));
        });
    }

    @Test
    void agrupaPorGrupoConLosBasicosAlFinalYCuentaCadaComidaDeLaMismaReceta() {
        Instant manana = AHORA.plus(Duration.ofDays(1));
        comidas(
                comida(1, POLLO, manana),
                comida(2, TARTA, manana.plus(Duration.ofHours(6))),
                comida(3, POLLO, manana.plus(Duration.ofDays(1))));
        tarjetas(tarjeta(POLLO, true, false, false, "autor"), tarjeta(TARTA, true, false, false, "autor"));
        GrupoDto condimentos = new GrupoDto(20, "Aceites y condimentos", null);
        when(plan.ingredientesDeRecetas(any()))
                .thenReturn(List.of(
                        ingrediente(TARTA, 41, "Chocolate", "100 gramos", DULCES, false),
                        ingrediente(POLLO, 30, "Sal", "1 pizca", condimentos, true),
                        ingrediente(POLLO, 10, "Pollo", "1 unidad", CARNE, false),
                        ingrediente(TARTA, 40, "Azúcar", "200 gramos", DULCES, false),
                        ingrediente(TARTA, 30, "Sal", "al gusto", condimentos, true)));

        ListaCompraDto lista = servicio.listaCompra(DESDE, HASTA, "lucia");

        assertThat(lista.comidas()).extracting(ComidaCompraDto::id).containsExactly(1L, 2L, 3L);
        assertThat(lista.secciones())
                .extracting(seccion -> seccion.grupo().nombre())
                .containsExactly("Carne", "Dulces", "Aceites y condimentos");
        assertThat(lista.secciones().getLast().basico()).isTrue();
        assertThat(lista.secciones().get(1).articulos())
                .extracting(ArticuloCompraDto::nombre)
                .containsExactly("Azúcar", "Chocolate");
        // El pollo se come dos veces
        assertThat(lista.secciones().getFirst().articulos().getFirst().total()).isEqualTo("2 unidades");
        ArticuloCompraDto sal = lista.secciones().getLast().articulos().getFirst();
        assertThat(sal.total()).isNull();
        assertThat(sal.cantidades())
                .containsExactly(
                        new CantidadCompraDto(POLLO, "Receta 1", "1 pizca", 2),
                        new CantidadCompraDto(TARTA, "Receta 2", "al gusto", 1));
    }

    @Test
    void sinComidasLaListaEstaVaciaYNoSeConsultanIngredientes() {
        comidas();
        tarjetas();

        ListaCompraDto lista = servicio.listaCompra(DESDE, HASTA, "lucia");

        assertThat(lista.comidas()).isEmpty();
        assertThat(lista.secciones()).isEmpty();
        assertThat(lista.noDisponibles()).isZero();
        verify(plan, never()).ingredientesDeRecetas(any());
    }

    @Test
    void unaSemanaFuturaEmpiezaEnSuPrincipio() {
        Instant desde = AHORA.plus(Duration.ofDays(7));
        comidas();
        tarjetas();

        servicio.listaCompra(desde, desde.plus(Duration.ofDays(7)), "lucia");

        verify(plan).comidas("lucia", desde, desde.plus(Duration.ofDays(7)));
    }

    private void consumosReales(ConsumoGrupo... filas) {
        lenient()
                .when(alimentacion.consumosConGrupos(eq("lucia"), any(), any()))
                .thenReturn(new ArrayList<>(List.of(filas)));
    }

    private void comidas(ComidaPlanificada... filas) {
        lenient().when(plan.comidas(eq("lucia"), any(), any())).thenReturn(List.of(filas));
    }

    private void tarjetas(RecetaResumenDto... lista) {
        Map<Integer, RecetaResumenDto> mapa = new java.util.HashMap<>();
        for (RecetaResumenDto receta : lista) {
            mapa.put(receta.id(), receta);
        }
        lenient().when(recetas.resumenes(any(), eq("lucia"))).thenReturn(mapa);
    }

    private static IngredienteDeReceta ingrediente(
            int receta, int id, String nombre, String cantidad, GrupoDto grupo, boolean basico) {
        return new IngredienteDeReceta(
                receta, id, nombre, cantidad, grupo.id(), grupo.nombre(), grupo.racionesSemana(), basico);
    }

    private static ComidaPlanificada comida(long id, int receta, Instant fecha) {
        return new ComidaPlanificada(id, "lucia", receta, fecha);
    }

    private static RecetaResumenDto tarjeta(
            int id, boolean publicada, boolean oculta, boolean eliminada, String autor) {
        return new RecetaResumenDto(
                id,
                "Receta " + id,
                autor,
                new TipoRecetaDto(1, "Plato"),
                null,
                20,
                2,
                "Fácil",
                0,
                false,
                null,
                0,
                publicada,
                AHORA,
                eliminada,
                oculta);
    }
}
