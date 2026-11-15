package es.alimentarse.config;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Cifrado de las contraseñas (D-090).
 *
 * @param costeBcrypt el coste de bcrypt, 12. Solo el perfil {@code test} puede bajarlo, para que los tests no tarden el
 *     doble: fuera de él, la aplicación no arranca con uno menor ({@code CifradoPassword.coste}).
 */
@Validated
@ConfigurationProperties("app.contrasenas")
public record ContrasenasProperties(
        @Min(value = 4, message = "bcrypt admite costes de 4 a 31")
        @Max(value = 31, message = "bcrypt admite costes de 4 a 31")
        int costeBcrypt) {}
