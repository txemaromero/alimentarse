package es.alimentarse.usuario;

import org.jspecify.annotations.Nullable;

/**
 * Aviso por correo de un cambio en el acceso a una cuenta (D-086), para que su dueño se entere si no ha sido él.
 *
 * @param email adónde se envía; si ha cambiado el email, al anterior
 * @param nuevoEmail el email nuevo, solo con {@link Motivo#EMAIL_CAMBIADO}
 */
public record AvisoCuenta(
        String nombre,
        String email,
        Motivo motivo,
        @Nullable String nuevoEmail) {

    public enum Motivo {
        EMAIL_CAMBIADO,
        GOOGLE_VINCULADO,
        GOOGLE_DESVINCULADO
    }
}
