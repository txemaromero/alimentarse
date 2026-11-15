package es.alimentarse.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Propiedades propias de la aplicación (prefijo {@code app}).
 *
 * @param baseUrl URL pública del frontend, usada en los enlaces de los correos
 * @param uploadDir directorio donde se guardan las imágenes subidas
 * @param mailFrom remitente de los correos que envía la aplicación
 * @param jwt firma y caducidad de los tokens de sesión
 * @param adminInicial usuario administrador que se crea en el primer arranque
 * @param limiteIntentos límites de peticiones por IP en los endpoints sensibles
 */
@Validated
@ConfigurationProperties(prefix = "app")
public record AppProperties(
        @NotBlank String baseUrl,
        @NotBlank String uploadDir,
        @NotBlank @Email String mailFrom,
        @NotNull @Valid Jwt jwt,
        @NotNull @Valid AdminInicial adminInicial,
        @NotNull @Valid LimiteIntentos limiteIntentos) {

    /**
     * @param secreto clave HMAC en Base64 (JWT_SECRET), de al menos 256 bits
     * @param expiracionHoras validez de cada token
     * @param cookieSegura la cookie de sesión solo viaja por HTTPS (COOKIE_SEGURA)
     */
    public record Jwt(
            @NotBlank(message = "JWT_SECRET es obligatorio: genera uno con 'openssl rand -base64 48'")
            String secreto,

            @Min(1) int expiracionHoras,

            boolean cookieSegura) {}

    /**
     * @param password contraseña inicial (ADMIN_INITIAL_PASSWORD); si está vacía, no se crea el administrador
     * @param email email del administrador (ADMIN_INITIAL_EMAIL)
     */
    public record AdminInicial(
            String password, @NotBlank @Email String email) {}

    /**
     * Límites por IP (D-086) y, el del login, también por cuenta (D-090).
     *
     * @param login el login y lo que comprueba la contraseña de la cuenta (cada uno con su contador)
     * @param olvidoPassword los correos de recuperación de la contraseña
     * @param registro las cuentas nuevas, con o sin Google
     * @param accesoGoogle los inicios del acceso con Google
     * @param loginCuenta las contraseñas fallidas de cada cuenta en el login, vengan de la IP que vengan (D-090)
     */
    public record LimiteIntentos(
            @NotNull @Valid Limite login,
            @NotNull @Valid Limite olvidoPassword,
            @NotNull @Valid Limite registro,
            @NotNull @Valid Limite accesoGoogle,
            @NotNull @Valid Limite loginCuenta) {}

    /**
     * @param maximo peticiones permitidas en cada ventana
     * @param ventana duración de la ventana
     */
    public record Limite(@Min(1) int maximo, @NotNull Duration ventana) {}
}
