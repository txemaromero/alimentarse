package es.alimentarse.push;

import static java.nio.charset.StandardCharsets.UTF_8;

import es.alimentarse.config.PushProperties;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Clock;
import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Envío real (D-078): cifra el mensaje para el navegador (RFC 8291), se identifica con VAPID (RFC 8292) y lo manda
 * por HTTP al servicio push. Nunca registra el endpoint completo, que da acceso a ese dispositivo: solo su dominio.
 */
@Component
class EnvioPushHttp implements EnvioPush {

    private static final Logger log = LoggerFactory.getLogger(EnvioPushHttp.class);
    private static final Duration ESPERA = Duration.ofSeconds(15);

    private final FirmaVapid firma;
    private final Clock reloj;
    private final HttpClient http;

    EnvioPushHttp(PushProperties propiedades, Clock reloj) {
        this.firma = propiedades.activos()
                ? new FirmaVapid(propiedades.clavePublica(), propiedades.clavePrivada(), propiedades.contacto())
                : null;
        this.reloj = reloj;
        this.http = HttpClient.newBuilder()
                .connectTimeout(ESPERA)
                .followRedirects(HttpClient.Redirect.NEVER)
                .build();
    }

    @Override
    public Resultado enviar(SuscripcionPush suscripcion, String mensaje, Duration validez) {
        if (firma == null) {
            return Resultado.ERROR;
        }
        URI endpoint = URI.create(suscripcion.endpoint());
        byte[] cuerpo = CifradoPush.cifrar(
                mensaje.getBytes(UTF_8),
                CifradoPush.base64Url(suscripcion.p256dh()),
                CifradoPush.base64Url(suscripcion.auth()));
        HttpRequest peticion = HttpRequest.newBuilder(endpoint)
                .timeout(ESPERA)
                .header("TTL", String.valueOf(Math.max(0, validez.toSeconds())))
                .header("Urgency", "normal")
                .header("Content-Encoding", "aes128gcm")
                .header("Content-Type", "application/octet-stream")
                .header("Authorization", firma.autorizacion(endpoint, reloj.instant()))
                .POST(HttpRequest.BodyPublishers.ofByteArray(cuerpo))
                .build();
        try {
            HttpResponse<String> respuesta = http.send(peticion, HttpResponse.BodyHandlers.ofString());
            int estado = respuesta.statusCode();
            if (estado >= 200 && estado < 300) {
                return Resultado.ENTREGADO;
            }
            if (estado == 404 || estado == 410) {
                return Resultado.CADUCADA;
            }
            log.warn("{} ha rechazado un aviso: {}", endpoint.getHost(), estado);
            return Resultado.ERROR;
        } catch (IOException e) {
            log.warn("No se ha podido enviar un aviso a {}: {}", endpoint.getHost(), e.toString());
            return Resultado.ERROR;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return Resultado.ERROR;
        }
    }
}
