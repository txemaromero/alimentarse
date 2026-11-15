package es.alimentarse.usuario;

/**
 * Hay que enviar el enlace para confirmar un email (D-088).
 *
 * @param email adónde: el de la cuenta o, si es un cambio, el nuevo
 * @param token el token en claro, que va en el enlace
 * @param cambio si es un email nuevo que sustituirá al de la cuenta
 */
public record ConfirmacionEmailSolicitada(String nombre, String email, String token, boolean cambio) {}
