package es.alimentarse.admin;

import org.jspecify.annotations.Nullable;

/**
 * Fila de la tabla de gestión de usuarios.
 *
 * @param admin si tiene el rol ADMIN
 * @param bloqueado si la administración lo ha bloqueado (D-063)
 */
public record UsuarioAdminDto(
        String id,
        String nombre,
        String apellido1,
        @Nullable String apellido2,
        String email,
        boolean admin,
        boolean bloqueado) {}
