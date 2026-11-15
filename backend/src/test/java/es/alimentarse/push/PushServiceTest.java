package es.alimentarse.push;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import es.alimentarse.comun.error.ExcepcionApi;
import es.alimentarse.config.PushProperties;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/** Avisos push (D-078). */
@ExtendWith(MockitoExtension.class)
class PushServiceTest {

    // Viernes 13 a las 12:30 en Madrid (UTC+1)
    private static final Instant AHORA = Instant.parse("2026-11-13T11:30:00.400Z");
    private static final String FCM = "https://fcm.googleapis.com/fcm/send/abc";
    private static final JsonMapper JSON = JsonMapper.builder().build();

    @Mock
    PushMapper push;

    @Mock
    EnvioPush envio;

    private PushService servicio;

    @BeforeEach
    void crearServicio() {
        servicio = servicio(true);
    }

    @Test
    void daLaClavePublicaSoloSiHayAvisos() {
        assertThat(servicio.clave().clavePublica()).isEqualTo(CifradoPushTest.SERVIDOR_PUBLICA);
        assertThat(servicio(false).clave().clavePublica()).isNull();
    }

    @Test
    void guardaLaSuscripcionConSuHashYSuZona() {
        servicio.suscribir("ana", solicitud(" " + FCM + " ", "Atlantic/Canary"));

        verify(push)
                .guardar(
                        "ana",
                        FCM,
                        PushService.hash(FCM),
                        CifradoPushTest.NAVEGADOR_PUBLICA,
                        CifradoPushTest.AUTH,
                        "Atlantic/Canary",
                        Instant.parse("2026-11-13T11:30:00Z"));
        assertThat(PushService.hash(FCM)).hasSize(64).isLowerCase();
    }

    @Test
    void sinZonaUsaLaDeMadrid() {
        servicio.suscribir("ana", solicitud(FCM, null));
        verify(push).guardar(eq("ana"), eq(FCM), anyString(), anyString(), anyString(), eq("Europe/Madrid"), any());
    }

    @Test
    void soloAdmiteLosServiciosPushConocidosPorHttps() {
        assertThat(servicio.servicioAdmitido(FCM)).isTrue();
        assertThat(servicio.servicioAdmitido("https://wns2-par02p.notify.windows.com/w/?token=x"))
                .isTrue();
        assertThat(servicio.servicioAdmitido("https://web.push.apple.com/abc")).isTrue();
        assertThat(servicio.servicioAdmitido("https://FCM.googleapis.com:443/fcm/send/abc"))
                .isTrue();

        assertThat(servicio.servicioAdmitido("http://fcm.googleapis.com/fcm/send/abc"))
                .isFalse();
        assertThat(servicio.servicioAdmitido("https://fcm.googleapis.com:8443/fcm/send/abc"))
                .isFalse();
        assertThat(servicio.servicioAdmitido("https://evilfcm.googleapis.com.example.org/x"))
                .isFalse();
        assertThat(servicio.servicioAdmitido("https://notfcm.googleapis.com.evil/x"))
                .isFalse();
        assertThat(servicio.servicioAdmitido("https://usuario@fcm.googleapis.com/x"))
                .isFalse();
        assertThat(servicio.servicioAdmitido("https://localhost/x")).isFalse();
        assertThat(servicio.servicioAdmitido("https://192.168.1.10/x")).isFalse();
        assertThat(servicio.servicioAdmitido("no es una url")).isFalse();
    }

    @Test
    void rechazaLasSuscripcionesNoValidasConUnErrorPorCampo() {
        assertThatThrownBy(() -> servicio.suscribir(
                        "ana", new SuscripcionPushRequest("https://localhost/push", "AAAA", "corto", "Marte/Olympus")))
                .isInstanceOfSatisfying(ExcepcionApi.class, e -> {
                    assertThat(e.getEstado()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(e.getErrores()).containsOnlyKeys("endpoint", "p256dh", "auth", "zona");
                });
        verify(push, never()).guardar(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void rechazaUnaClaveDelNavegadorQueNoEsUnPuntoDeLaCurva() {
        byte[] punto = CifradoPush.base64Url(CifradoPushTest.NAVEGADOR_PUBLICA);
        punto[64] ^= 1;

        assertThatThrownBy(() -> servicio.suscribir(
                        "ana",
                        new SuscripcionPushRequest(FCM, CifradoPush.base64Url(punto), CifradoPushTest.AUTH, null)))
                .isInstanceOfSatisfying(
                        ExcepcionApi.class, e -> assertThat(e.getErrores()).containsOnlyKeys("p256dh"));
        verify(push, never()).guardar(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void comoMuchoDiezNavegadoresSinContarElQueSeVuelveASuscribir() {
        when(push.contarOtros("ana", PushService.hash(FCM))).thenReturn(PushService.MAXIMO_NAVEGADORES);

        assertThatThrownBy(() -> servicio.suscribir("ana", solicitud(FCM, null)))
                .isInstanceOfSatisfying(ExcepcionApi.class, e -> {
                    assertThat(e.getEstado()).isEqualTo(HttpStatus.CONFLICT);
                    assertThat(e.getCodigo()).isEqualTo("LIMITE_NAVEGADORES");
                });
        verify(push, never()).guardar(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void sinClavesNoSeSuscribeNiSePrueba() {
        PushService sinAvisos = servicio(false);
        assertThatThrownBy(() -> sinAvisos.suscribir("ana", solicitud(FCM, null)))
                .isInstanceOfSatisfying(
                        ExcepcionApi.class, e -> assertThat(e.getCodigo()).isEqualTo("AVISOS_DESACTIVADOS"));
        assertThatThrownBy(() -> sinAvisos.probar("ana")).isInstanceOf(ExcepcionApi.class);
        sinAvisos.avisarComidas();
        verifyNoInteractions(push, envio);
    }

    @Test
    void desuscribirBorraPorElHash() {
        servicio.desuscribir("ana", FCM);
        verify(push).borrar("ana", PushService.hash(FCM));
    }

    @Test
    void laPruebaLlegaATodosSusNavegadoresYBorraLasCaducadas() {
        SuscripcionPush movil = suscripcion(1, FCM);
        SuscripcionPush antigua = suscripcion(2, "https://updates.push.services.mozilla.com/wpush/v2/x");
        when(push.deUsuario("ana")).thenReturn(List.of(movil, antigua));
        when(envio.enviar(eq(movil), anyString(), eq(PushService.VALIDEZ_PRUEBA)))
                .thenReturn(EnvioPush.Resultado.ENTREGADO);
        when(envio.enviar(eq(antigua), anyString(), eq(PushService.VALIDEZ_PRUEBA)))
                .thenReturn(EnvioPush.Resultado.CADUCADA);

        assertThat(servicio.probar("ana").enviados()).isEqualTo(1);
        verify(push).borrarPorId(2);
    }

    @Test
    void noEnviaAUnServicioQueYaNoSeAdmite() {
        when(push.deUsuario("ana")).thenReturn(List.of(suscripcion(3, "https://push.example.org/x")));

        assertThat(servicio.probar("ana").enviados()).isZero();
        verifyNoInteractions(envio);
    }

    @Test
    void unNavegadorQueFallaNoDejaSinAvisoALosDemas() {
        Instant hora = Instant.parse("2026-11-13T13:00:00Z");
        ComidaPorAvisar deAna = new ComidaPorAvisar(7, "ana", 3, "Lentejas", hora);
        ComidaPorAvisar deLuis = new ComidaPorAvisar(8, "luis", 4, "Tortilla", hora);
        when(push.comidasPorAvisar(AHORA, AHORA.plus(Duration.ofMinutes(60)))).thenReturn(List.of(deAna, deLuis));
        when(push.reclamar(anyLong(), any())).thenReturn(1);
        SuscripcionPush rota = suscripcion(1, FCM);
        SuscripcionPush deLuisBuena = new SuscripcionPush(
                2,
                "luis",
                "https://fcm.googleapis.com/fcm/send/luis",
                CifradoPushTest.NAVEGADOR_PUBLICA,
                CifradoPushTest.AUTH,
                "Europe/Madrid",
                AHORA);
        when(push.deUsuario("ana")).thenReturn(List.of(rota));
        when(push.deUsuario("luis")).thenReturn(List.of(deLuisBuena));
        when(envio.enviar(eq(rota), anyString(), any()))
                .thenThrow(new IllegalStateException("No se ha podido cifrar el aviso"));
        when(envio.enviar(eq(deLuisBuena), anyString(), any())).thenReturn(EnvioPush.Resultado.ENTREGADO);

        servicio.avisarComidas();

        verify(envio).enviar(eq(deLuisBuena), anyString(), any());
        verify(push, never()).borrarPorId(anyLong());
    }

    @Test
    void sinNingunNavegadorLaPruebaEsUnConflicto() {
        when(push.deUsuario("ana")).thenReturn(List.of());
        assertThatThrownBy(() -> servicio.probar("ana"))
                .isInstanceOfSatisfying(
                        ExcepcionApi.class, e -> assertThat(e.getCodigo()).isEqualTo("SIN_SUSCRIPCIONES"));
    }

    @Test
    void avisaUnaVezDeCadaComidaQueEmpiezaDentroDeLaAntelacion() {
        Instant comidaA = Instant.parse("2026-11-13T13:00:00Z");
        ComidaPorAvisar comida = new ComidaPorAvisar(7, "ana", 3, "Lentejas", comidaA);
        ComidaPorAvisar yaReclamada = new ComidaPorAvisar(8, "ana", 4, "Tortilla", comidaA);
        when(push.comidasPorAvisar(AHORA, AHORA.plus(Duration.ofMinutes(60)))).thenReturn(List.of(comida, yaReclamada));
        when(push.reclamar(7, Instant.parse("2026-11-13T11:30:00Z"))).thenReturn(1);
        when(push.reclamar(8, Instant.parse("2026-11-13T11:30:00Z"))).thenReturn(0);
        SuscripcionPush navegador = suscripcion(1, FCM);
        when(push.deUsuario("ana")).thenReturn(List.of(navegador));
        when(envio.enviar(eq(navegador), anyString(), any())).thenReturn(EnvioPush.Resultado.ENTREGADO);

        servicio.avisarComidas();

        // Hasta la hora de la comida: después, el aviso ya no sirve
        verify(envio)
                .enviar(
                        navegador,
                        servicio.recordatorio(comida, "Europe/Madrid", AHORA),
                        Duration.ofSeconds(5399, 600_000_000));
        verify(push, never()).borrarPorId(anyLong());
    }

    @Test
    void elRecordatorioDiceQueComidaEsYAQueHoraEnLaZonaDelNavegador() {
        JsonNode aviso = JSON.readTree(servicio.recordatorio(
                        new ComidaPorAvisar(7, "ana", 3, "Lentejas estofadas", Instant.parse("2026-11-13T13:00:00Z")),
                        "Europe/Madrid",
                        AHORA))
                .get("notification");

        assertThat(aviso.get("title").asString()).isEqualTo("Comida: Lentejas estofadas");
        assertThat(aviso.get("body").asString()).isEqualTo("Hoy a las 14:00, según tu planificador.");
        assertThat(aviso.get("icon").asString()).isEqualTo(PushService.ICONO);
        assertThat(aviso.get("tag").asString()).isEqualTo("comida-7");
        assertThat(aviso.at("/data/onActionClick/default/operation").asString()).isEqualTo("navigateLastFocusedOrOpen");
        assertThat(aviso.at("/data/onActionClick/default/url").asString()).isEqualTo("/recetas/3");
    }

    @Test
    void laCenaDeMananaLoDiceYUnaZonaQueNoExisteUsaLaDeMadrid() {
        // Jueves a las 23:50 en Madrid; la cena es la del viernes a las 21:00
        Instant ahora = Instant.parse("2026-11-12T22:50:00Z");
        JsonNode aviso = JSON.readTree(servicio.recordatorio(
                        new ComidaPorAvisar(9, "ana", 5, "Crema", Instant.parse("2026-11-13T20:00:00Z")),
                        "Marte/Olympus",
                        ahora))
                .get("notification");

        assertThat(aviso.get("title").asString()).isEqualTo("Cena: Crema");
        assertThat(aviso.get("body").asString()).isEqualTo("Mañana a las 21:00, según tu planificador.");
    }

    private PushService servicio(boolean conClaves) {
        PushProperties propiedades = new PushProperties(
                conClaves ? CifradoPushTest.SERVIDOR_PUBLICA : null,
                conClaves ? CifradoPushTest.SERVIDOR_PRIVADA : null,
                "mailto:pruebas@toca.local",
                Duration.ofMinutes(60),
                List.of(
                        "fcm.googleapis.com",
                        "updates.push.services.mozilla.com",
                        "notify.windows.com",
                        "push.apple.com"));
        return new PushService(propiedades, push, envio, JSON, Clock.fixed(AHORA, ZoneOffset.UTC));
    }

    private static SuscripcionPushRequest solicitud(String endpoint, String zona) {
        return new SuscripcionPushRequest(endpoint, CifradoPushTest.NAVEGADOR_PUBLICA, CifradoPushTest.AUTH, zona);
    }

    private static SuscripcionPush suscripcion(long id, String endpoint) {
        return new SuscripcionPush(
                id, "ana", endpoint, CifradoPushTest.NAVEGADOR_PUBLICA, CifradoPushTest.AUTH, "Europe/Madrid", AHORA);
    }
}
