package es.alimentarse.auth;

/**
 * Resultado del login.
 *
 * @param token JWT que va en la cookie de sesión; nunca en el cuerpo de la respuesta (D-070)
 * @param respuesta lo que recibe el frontend
 */
public record SesionIniciada(String token, LoginResponse respuesta) {}
