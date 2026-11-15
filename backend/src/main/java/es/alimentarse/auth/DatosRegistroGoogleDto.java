package es.alimentarse.auth;

import org.jspecify.annotations.Nullable;

/**
 * Lo que ya se sabe de quien termina de registrarse con Google (D-085), para rellenar el formulario. El email no se
 * puede cambiar: es el de su cuenta de Google.
 *
 * @param apellido1 el primer apellido que da Google, o vacío
 * @param apellido2 el resto de los apellidos que da Google, o {@code null}
 */
public record DatosRegistroGoogleDto(
        String email,
        String nombre,
        String apellido1,
        @Nullable String apellido2) {}
