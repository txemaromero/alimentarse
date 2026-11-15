package es.alimentarse.usuario;

import java.time.Instant;

/**
 * Fila de la tabla {@code usuarios}.
 *
 * @param id nombre de usuario
 * @param password hash BCrypt
 * @param tokenPassword SHA-256 del token de recuperación de contraseña pendiente, o {@code null}
 * @param tokenExpiracion caducidad de ese token, o {@code null}
 * @param bloqueado bloqueado por la administración: no puede iniciar sesión (D-063)
 * @param googleId la cuenta de Google con la que puede entrar («sub»), o {@code null} (D-085)
 * @param emailVerificado su dueño ha confirmado el email con el enlace, o lo ha verificado Google (D-088)
 * @param emailPendiente el email nuevo que espera su confirmación, o {@code null} (D-088)
 */
public record Usuario(
        String id,
        String nombre,
        String apellido1,
        String apellido2,
        String email,
        String password,
        String tokenPassword,
        Instant tokenExpiracion,
        boolean bloqueado,
        String googleId,
        boolean emailVerificado,
        String emailPendiente) {

    public static Usuario nuevo(
            String id, String nombre, String apellido1, String apellido2, String email, String passwordCifrada) {
        return new Usuario(
                id, nombre, apellido1, apellido2, email, passwordCifrada, null, null, false, null, false, null);
    }
}
