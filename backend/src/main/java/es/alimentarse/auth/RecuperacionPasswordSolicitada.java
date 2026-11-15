package es.alimentarse.auth;

/**
 * Evento: se ha generado un token de recuperación de contraseña. Tras el commit se envía por correo.
 *
 * @param token el token en claro, que solo viaja en el correo
 */
public record RecuperacionPasswordSolicitada(String nombre, String email, String token) {

    @Override
    public String toString() {
        return "RecuperacionPasswordSolicitada[email=" + email + "]";
    }
}
