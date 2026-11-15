package es.alimentarse.push;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.jspecify.annotations.Nullable;

/**
 * La suscripción push de un navegador (D-078), tal como la da {@code PushSubscription.toJSON()}, más su zona horaria.
 *
 * @param p256dh {@code keys.p256dh}: clave pública del navegador, en Base64 URL
 * @param auth {@code keys.auth}: secreto de autenticación, en Base64 URL
 * @param zona zona horaria IANA del navegador (por ejemplo, {@code Europe/Madrid}); si falta, la de Madrid
 */
public record SuscripcionPushRequest(
        @NotBlank(message = "Falta el endpoint") @Size(max = 1000, message = "El endpoint es demasiado largo")
        String endpoint,

        @NotBlank(message = "Falta la clave del navegador")
        @Size(max = 100, message = "La clave del navegador no es válida")
        String p256dh,

        @NotBlank(message = "Falta el secreto del navegador")
        @Size(max = 50, message = "El secreto del navegador no es válido")
        String auth,

        @Nullable @Size(max = 50, message = "La zona horaria no es válida")
        String zona) {}
