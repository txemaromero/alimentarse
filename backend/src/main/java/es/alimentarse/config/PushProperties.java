package es.alimentarse.config;

import java.time.Duration;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Avisos push del recordatorio de las comidas planificadas (D-078, prefijo {@code app.push}). Sin las dos claves VAPID,
 * los avisos están desactivados: la aplicación funciona igual, sin ellos.
 *
 * @param clavePublica clave pública VAPID (PUSH_CLAVE_PUBLICA): punto P-256 sin comprimir, en Base64 URL sin relleno
 * @param clavePrivada clave privada VAPID (PUSH_CLAVE_PRIVADA): los 32 bytes del escalar, en Base64 URL sin relleno
 * @param contacto quién envía los avisos, para el servicio push (PUSH_CONTACTO): {@code mailto:} o {@code https:}
 * @param antelacion cuánto antes de la comida se avisa
 * @param servicios dominios de los servicios push admitidos: el servidor solo envía avisos a ellos
 */
@ConfigurationProperties(prefix = "app.push")
public record PushProperties(
        String clavePublica,
        String clavePrivada,
        @DefaultValue("mailto:no-reply@toca.local") String contacto,
        @DefaultValue("60m") Duration antelacion,

        @DefaultValue({"fcm.googleapis.com", "updates.push.services.mozilla.com", "notify.windows.com", "push.apple.com"
        })
        List<String> servicios) {

    public boolean activos() {
        return clavePublica != null && !clavePublica.isBlank() && clavePrivada != null && !clavePrivada.isBlank();
    }
}
