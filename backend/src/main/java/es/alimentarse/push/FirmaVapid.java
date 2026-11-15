package es.alimentarse.push;

import static java.nio.charset.StandardCharsets.US_ASCII;
import static java.nio.charset.StandardCharsets.UTF_8;

import java.net.URI;
import java.security.GeneralSecurityException;
import java.security.Signature;
import java.security.interfaces.ECPrivateKey;
import java.time.Duration;
import java.time.Instant;

/**
 * Identificación del servidor ante el servicio push (D-078, VAPID, RFC 8292): un JWT ES256 para el origen del
 * endpoint, firmado con la clave privada VAPID, y la clave pública que lo verifica.
 */
final class FirmaVapid {

    /** El servicio push no admite más de 24 horas; 12 es lo habitual. */
    static final Duration VALIDEZ = Duration.ofHours(12);

    private static final String CABECERA_JWT =
            CifradoPush.base64Url("{\"typ\":\"JWT\",\"alg\":\"ES256\"}".getBytes(UTF_8));

    private final ECPrivateKey clavePrivada;
    private final String clavePublica;
    private final String contacto;

    FirmaVapid(String clavePublica, String clavePrivada, String contacto) {
        try {
            // Comprueba que las dos claves tienen el formato de Web Push
            CifradoPush.clavePublica(CifradoPush.base64Url(clavePublica));
            this.clavePrivada = CifradoPush.clavePrivada(CifradoPush.base64Url(clavePrivada));
        } catch (GeneralSecurityException | IllegalArgumentException e) {
            throw new IllegalStateException(
                    "Las claves VAPID (PUSH_CLAVE_PUBLICA, PUSH_CLAVE_PRIVADA) no son válidas", e);
        }
        if (!contacto.startsWith("mailto:") && !contacto.startsWith("https:")) {
            throw new IllegalStateException("PUSH_CONTACTO debe empezar por mailto: o https:");
        }
        this.clavePublica = clavePublica.strip();
        this.contacto = contacto;
    }

    /** Valor de la cabecera {@code Authorization} para enviar un aviso a ese endpoint. */
    String autorizacion(URI endpoint, Instant ahora) {
        String origen = endpoint.getScheme() + "://" + endpoint.getHost()
                + (endpoint.getPort() == -1 ? "" : ":" + endpoint.getPort());
        String reclamaciones = "{\"aud\":\"" + json(origen) + "\",\"exp\":"
                + ahora.plus(VALIDEZ).getEpochSecond() + ",\"sub\":\"" + json(contacto) + "\"}";
        String sinFirmar = CABECERA_JWT + "." + CifradoPush.base64Url(reclamaciones.getBytes(UTF_8));
        try {
            // Firma en el formato de JOSE: R ‖ S, 64 bytes, no DER
            Signature firma = Signature.getInstance("SHA256withECDSAinP1363Format");
            firma.initSign(clavePrivada);
            firma.update(sinFirmar.getBytes(US_ASCII));
            return "vapid t=" + sinFirmar + "." + CifradoPush.base64Url(firma.sign()) + ", k=" + clavePublica;
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("No se ha podido firmar el aviso", e);
        }
    }

    /** Escapa las comillas y las barras: el origen y el contacto no llevan nada más que haya que escapar. */
    private static String json(String texto) {
        return texto.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
