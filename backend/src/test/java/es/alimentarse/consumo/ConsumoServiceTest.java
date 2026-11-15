package es.alimentarse.consumo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import es.alimentarse.comun.Pagina;
import es.alimentarse.comun.error.ExcepcionApi;
import es.alimentarse.receta.RecetaService;
import es.alimentarse.seguridad.LimiteIntentosService;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

@ExtendWith(MockitoExtension.class)
class ConsumoServiceTest {

    /** Con milisegundos, para comprobar que se truncan. */
    private static final Instant AHORA = Instant.parse("2026-11-05T12:00:00.750Z");

    private static final Instant AHORA_EN_SEGUNDOS = Instant.parse("2026-11-05T12:00:00Z");

    @Mock
    ConsumoMapper consumos;

    @Mock
    RecetaService recetas;

    @Mock
    LimiteIntentosService limite;

    private ConsumoService servicio;

    @BeforeEach
    void crearServicio() {
        servicio = new ConsumoService(consumos, recetas, limite, Clock.fixed(AHORA, ZoneOffset.UTC));
    }

    @Test
    void sinFechaSeRegistraAhoraSinFraccionesDeSegundo() {
        prepararConsumoCreado();

        servicio.registrar(new ConsumoRequest(7, null), "lucia");

        verify(recetas).exigirVisible(7, "lucia");
        verify(limite).registrarIntento("consumo:lucia", ConsumoService.LIMITE);
        verify(consumos).insertar("lucia", 7, AHORA_EN_SEGUNDOS);
    }

    /** Como mucho 50 al día (D-090): 429 con su código y sin guardar nada. */
    @Test
    void pasadoElTopeDiarioNoSeRegistra() {
        doThrow(ExcepcionApi.demasiadosIntentos(Duration.ofHours(3)))
                .when(limite)
                .registrarIntento("consumo:lucia", ConsumoService.LIMITE);

        assertThatThrownBy(() -> servicio.registrar(new ConsumoRequest(7, null), "lucia"))
                .isInstanceOfSatisfying(ExcepcionApi.class, e -> {
                    assertThat(e.getEstado()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
                    assertThat(e.getCodigo()).isEqualTo("LIMITE_CONSUMOS");
                    assertThat(e.getReintentarTras()).isEqualTo(Duration.ofHours(3));
                });
        verify(consumos, never()).insertar(anyString(), anyInt(), any());
    }

    @Test
    void unaPeticionNoValidaNoCuentaEnElTope() {
        assertThatThrownBy(() -> servicio.registrar(new ConsumoRequest(7, AHORA.plusSeconds(3600)), "lucia"))
                .isInstanceOf(ExcepcionApi.class);

        verify(limite, never()).registrarIntento(anyString(), any());
    }

    @Test
    void unaFechaPasadaSeGuardaTruncadaASegundos() {
        prepararConsumoCreado();

        servicio.registrar(new ConsumoRequest(7, Instant.parse("2026-11-01T10:15:30.999Z")), "lucia");

        verify(consumos).insertar("lucia", 7, Instant.parse("2026-11-01T10:15:30Z"));
    }

    @Test
    void unaFechaFuturaSeRechaza() {
        assertThatThrownBy(() -> servicio.registrar(
                        new ConsumoRequest(7, AHORA.plusSeconds(ConsumoService.MARGEN_RELOJ.toSeconds() + 1)), "lucia"))
                .isInstanceOfSatisfying(ExcepcionApi.class, e -> {
                    assertThat(e.getEstado()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(e.getErrores()).containsEntry("fecha", ConsumoService.FECHA_FUTURA);
                });
        verify(consumos, never()).insertar(anyString(), anyInt(), any());
    }

    @Test
    void unaFechaFuturaDentroDelMargenDeRelojSeGuardaComoAhora() {
        prepararConsumoCreado();

        servicio.registrar(new ConsumoRequest(7, AHORA.plusSeconds(30)), "lucia");

        verify(consumos).insertar("lucia", 7, AHORA_EN_SEGUNDOS);
    }

    @Test
    void noSePuedeRegistrarUnaRecetaQueElUsuarioNoVe() {
        doThrow(ExcepcionApi.noEncontrado("La receta no existe.")).when(recetas).exigirVisible(2, "lucia");

        assertThatThrownBy(() -> servicio.registrar(new ConsumoRequest(2, null), "lucia"))
                .isInstanceOfSatisfying(
                        ExcepcionApi.class, e -> assertThat(e.getEstado()).isEqualTo(HttpStatus.NOT_FOUND));
        verify(consumos, never()).insertar(anyString(), anyInt(), any());
    }

    @Test
    void soloSePuedenCambiarYBorrarLosConsumosPropios() {
        when(consumos.buscarPorId(5)).thenReturn(Optional.of(new Consumo(5, "marcos", 7, AHORA)));

        assertThatThrownBy(() -> servicio.cambiarFecha(5, new FechaConsumoRequest(AHORA), "lucia"))
                .isInstanceOfSatisfying(
                        ExcepcionApi.class, e -> assertThat(e.getEstado()).isEqualTo(HttpStatus.NOT_FOUND));
        assertThatThrownBy(() -> servicio.borrar(5, "lucia")).isInstanceOf(ExcepcionApi.class);
        verify(consumos, never()).actualizarFecha(anyLong(), any());
        verify(consumos, never()).borrar(anyLong());
    }

    @Test
    void cambiarLaFechaDeUnConsumoPropio() {
        when(consumos.buscarPorId(5)).thenReturn(Optional.of(new Consumo(5, "lucia", 7, AHORA)));
        when(consumos.buscarDto(5, "LUCIA")).thenReturn(Optional.of(new ConsumoDto(5, AHORA, null)));

        servicio.cambiarFecha(5, new FechaConsumoRequest(Instant.parse("2026-11-04T08:00:00Z")), "LUCIA");

        verify(consumos).actualizarFecha(5, Instant.parse("2026-11-04T08:00:00Z"));
    }

    @Test
    void cambiarAUnaFechaFuturaSeRechaza() {
        when(consumos.buscarPorId(5)).thenReturn(Optional.of(new Consumo(5, "lucia", 7, AHORA)));

        assertThatThrownBy(() -> servicio.cambiarFecha(5, new FechaConsumoRequest(AHORA.plusSeconds(3600)), "lucia"))
                .isInstanceOfSatisfying(
                        ExcepcionApi.class, e -> assertThat(e.getErrores()).containsKey("fecha"));
    }

    @Test
    void borrarUnConsumoPropio() {
        when(consumos.buscarPorId(5)).thenReturn(Optional.of(new Consumo(5, "lucia", 7, AHORA)));

        servicio.borrar(5, "lucia");

        verify(consumos).borrar(5);
    }

    @Test
    void elHistorialSePaginaYNoConsultaFilasFueraDeRango() {
        ConsumoDto consumo = new ConsumoDto(1, AHORA, null);
        when(consumos.contar("lucia")).thenReturn(10L);
        when(consumos.historial("lucia", 9, 9)).thenReturn(List.of(consumo));

        assertThat(servicio.historial("lucia", 1, 9)).isEqualTo(new Pagina<>(List.of(consumo), 1, 9, 10, 2));
        assertThat(servicio.historial("lucia", 5, 9).contenido()).isEmpty();
        verify(consumos, never()).historial("lucia", 45, 9);
    }

    @Test
    void losUltimosSonLaPrimeraPaginaDelHistorial() {
        when(consumos.historial("lucia", 0, 3)).thenReturn(List.of());

        assertThat(servicio.ultimos("lucia", 3)).isEmpty();
    }

    private void prepararConsumoCreado() {
        when(consumos.ultimoIdInsertado()).thenReturn(1L);
        when(consumos.buscarDto(1, "lucia")).thenReturn(Optional.of(new ConsumoDto(1, AHORA, null)));
    }
}
