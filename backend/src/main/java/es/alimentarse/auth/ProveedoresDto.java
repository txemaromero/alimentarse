package es.alimentarse.auth;

/**
 * Las formas de entrar además del usuario y la contraseña.
 *
 * @param google si está configurado el acceso con Google (D-085)
 */
public record ProveedoresDto(boolean google) {}
