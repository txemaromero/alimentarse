package es.alimentarse.auth;

/**
 * Lo que dice Google de quien acaba de entrar con su cuenta (D-085), sacado del token de OpenID Connect.
 *
 * @param id el identificador estable de la cuenta («sub»)
 * @param emailVerificado si Google ha comprobado que el email es suyo («email_verified»)
 * @param nombre «given_name», o vacío
 * @param apellidos «family_name», o vacío
 */
public record CuentaGoogle(String id, String email, boolean emailVerificado, String nombre, String apellidos) {}
