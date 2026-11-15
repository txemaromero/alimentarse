package es.alimentarse.config;

import org.jspecify.annotations.Nullable;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Acceso con Google (D-085), con las credenciales de un cliente OAuth de Google Cloud Console. Sin ellas, el acceso con
 * Google está desactivado: no aparece el botón y sus rutas no existen.
 *
 * @param clientId el ID de cliente ({@code GOOGLE_CLIENT_ID})
 * @param clientSecret su secreto ({@code GOOGLE_CLIENT_SECRET})
 */
@ConfigurationProperties("app.google")
public record GoogleProperties(
        @Nullable String clientId, @Nullable String clientSecret) {

    public boolean habilitado() {
        return clientId != null && !clientId.isBlank() && clientSecret != null && !clientSecret.isBlank();
    }
}
