package es.alimentarse.usuario;

/**
 * Lo que hay que comprobar de una cuenta en cada petición (D-063, D-070).
 *
 * @param version sube al cambiar la contraseña o el rol: los tokens con otra versión dejan de valer
 */
public record EstadoSesion(boolean bloqueado, int version) {}
