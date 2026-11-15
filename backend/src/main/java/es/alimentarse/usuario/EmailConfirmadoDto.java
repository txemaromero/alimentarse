package es.alimentarse.usuario;

/**
 * El email ya está confirmado (D-088).
 *
 * @param usuario el id de la cuenta
 * @param email el email confirmado, que es ya el de la cuenta
 */
public record EmailConfirmadoDto(String usuario, String email) {}
