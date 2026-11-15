package es.alimentarse.push;

import static java.nio.charset.StandardCharsets.US_ASCII;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.net.URI;
import java.security.Signature;
import java.time.Instant;
import org.junit.jupiter.api.Test;

/** Identificación VAPID ante el servicio push (D-078, RFC 8292). */
class FirmaVapidTest {

    private static final Instant AHORA = Instant.parse("2026-11-13T20:00:00Z");

    private final FirmaVapid firma = new FirmaVapid(
            CifradoPushTest.SERVIDOR_PUBLICA, CifradoPushTest.SERVIDOR_PRIVADA, "mailto:pruebas@toca.local");

    @Test
    void firmaUnJwtEs256ParaElOrigenDelEndpoint() throws Exception {
        String autorizacion = firma.autorizacion(URI.create("https://fcm.googleapis.com/fcm/send/abc123"), AHORA);

        assertThat(autorizacion).startsWith("vapid t=").endsWith(", k=" + CifradoPushTest.SERVIDOR_PUBLICA);
        String jwt = autorizacion.substring("vapid t=".length(), autorizacion.indexOf(", k="));
        String[] partes = jwt.split("\\.");
        assertThat(partes).hasSize(3);
        assertThat(new String(CifradoPush.base64Url(partes[0]), UTF_8))
                .isEqualTo("{\"typ\":\"JWT\",\"alg\":\"ES256\"}");
        assertThat(new String(CifradoPush.base64Url(partes[1]), UTF_8))
                .isEqualTo("{\"aud\":\"https://fcm.googleapis.com\",\"exp\":"
                        + AHORA.plus(FirmaVapid.VALIDEZ).getEpochSecond()
                        + ",\"sub\":\"mailto:pruebas@toca.local\"}");

        Signature verificacion = Signature.getInstance("SHA256withECDSAinP1363Format");
        verificacion.initVerify(CifradoPush.clavePublica(CifradoPush.base64Url(CifradoPushTest.SERVIDOR_PUBLICA)));
        verificacion.update((partes[0] + "." + partes[1]).getBytes(US_ASCII));
        assertThat(verificacion.verify(CifradoPush.base64Url(partes[2]))).isTrue();
    }

    @Test
    void elOrigenLlevaElPuertoSiNoEsElPorDefecto() {
        String autorizacion = firma.autorizacion(URI.create("http://localhost:8123/push"), AHORA);
        String reclamaciones = autorizacion.split("\\.")[1];
        assertThat(new String(CifradoPush.base64Url(reclamaciones), UTF_8))
                .contains("\"aud\":\"http://localhost:8123\"");
    }

    @Test
    void exigeClavesValidasYUnContactoMailtoOHttps() {
        assertThatThrownBy(() -> new FirmaVapid("no-es-una-clave", CifradoPushTest.SERVIDOR_PRIVADA, "mailto:a@b.c"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("PUSH_CLAVE_PUBLICA");
        assertThatThrownBy(() -> new FirmaVapid(CifradoPushTest.SERVIDOR_PUBLICA, "Y29ydGE", "mailto:a@b.c"))
                .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> new FirmaVapid(
                        CifradoPushTest.SERVIDOR_PUBLICA, CifradoPushTest.SERVIDOR_PRIVADA, "pruebas@toca.local"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("PUSH_CONTACTO");
    }
}
