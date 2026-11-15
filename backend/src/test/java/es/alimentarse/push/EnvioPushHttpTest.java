package es.alimentarse.push;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpServer;
import es.alimentarse.config.PushProperties;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Envío real de un aviso (D-078), contra un servicio push simulado en este equipo. */
class EnvioPushHttpTest {

    private static final Clock RELOJ = Clock.fixed(Instant.parse("2026-11-13T20:00:00Z"), ZoneOffset.UTC);

    private HttpServer servidor;
    private final AtomicInteger estado = new AtomicInteger(201);
    private final AtomicReference<Headers> cabeceras = new AtomicReference<>();
    private final AtomicReference<byte[]> cuerpo = new AtomicReference<>();
    private String endpoint;

    @BeforeEach
    void arrancarServicioPush() throws Exception {
        servidor = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
        servidor.createContext("/push/", intercambio -> {
            cabeceras.set(intercambio.getRequestHeaders());
            cuerpo.set(intercambio.getRequestBody().readAllBytes());
            intercambio.sendResponseHeaders(estado.get(), -1);
            intercambio.close();
        });
        servidor.start();
        endpoint = "http://localhost:" + servidor.getAddress().getPort() + "/push/abc";
    }

    @AfterEach
    void pararServicioPush() {
        servidor.stop(0);
    }

    @Test
    void cifraElMensajeParaEseNavegadorYSeIdentificaConVapid() throws Exception {
        assertThat(envio(true).enviar(suscripcion(), "{\"notification\":{\"title\":\"Hola\"}}", Duration.ofMinutes(30)))
                .isEqualTo(EnvioPush.Resultado.ENTREGADO);

        assertThat(cabeceras.get().getFirst("TTL")).isEqualTo("1800");
        assertThat(cabeceras.get().getFirst("Content-Encoding")).isEqualTo("aes128gcm");
        assertThat(cabeceras.get().getFirst("Authorization"))
                .startsWith("vapid t=")
                .endsWith(", k=" + CifradoPushTest.SERVIDOR_PUBLICA);
        byte[] claro = DescifradoNavegador.descifrar(
                cuerpo.get(),
                CifradoPushTest.NAVEGADOR_PRIVADA,
                CifradoPushTest.NAVEGADOR_PUBLICA,
                CifradoPushTest.AUTH);
        assertThat(new String(claro, UTF_8)).isEqualTo("{\"notification\":{\"title\":\"Hola\"}}");
    }

    @Test
    void una404OUna410SignificanQueLaSuscripcionYaNoExiste() {
        estado.set(410);
        assertThat(envio(true).enviar(suscripcion(), "{}", Duration.ZERO)).isEqualTo(EnvioPush.Resultado.CADUCADA);
        estado.set(404);
        assertThat(envio(true).enviar(suscripcion(), "{}", Duration.ZERO)).isEqualTo(EnvioPush.Resultado.CADUCADA);
    }

    @Test
    void otrosFallosSonErroresYNoBorranNada() {
        estado.set(429);
        assertThat(envio(true).enviar(suscripcion(), "{}", Duration.ZERO)).isEqualTo(EnvioPush.Resultado.ERROR);

        servidor.stop(0);
        assertThat(envio(true).enviar(suscripcion(), "{}", Duration.ZERO)).isEqualTo(EnvioPush.Resultado.ERROR);
    }

    @Test
    void sinClavesNoEnviaNada() {
        assertThat(envio(false).enviar(suscripcion(), "{}", Duration.ZERO)).isEqualTo(EnvioPush.Resultado.ERROR);
        assertThat(cuerpo.get()).isNull();
    }

    private EnvioPushHttp envio(boolean conClaves) {
        PushProperties propiedades = new PushProperties(
                conClaves ? CifradoPushTest.SERVIDOR_PUBLICA : null,
                conClaves ? CifradoPushTest.SERVIDOR_PRIVADA : null,
                "mailto:pruebas@toca.local",
                Duration.ofMinutes(60),
                List.of("fcm.googleapis.com"));
        return new EnvioPushHttp(propiedades, RELOJ);
    }

    private SuscripcionPush suscripcion() {
        return new SuscripcionPush(
                1, "ana", endpoint, CifradoPushTest.NAVEGADOR_PUBLICA, CifradoPushTest.AUTH, "Europe/Madrid", null);
    }
}
