package es.alimentarse.auth;

import org.jspecify.annotations.Nullable;

/** Evento: se ha creado una cuenta. Tras el commit se envía el correo de bienvenida. */
/**
 * Se ha creado una cuenta: hay que enviar el correo de bienvenida.
 *
 * @param tokenConfirmacion el del enlace para confirmar el email, o {@code null} si ya está verificado (D-088)
 */
public record UsuarioRegistrado(
        String id, String nombre, String email, @Nullable String tokenConfirmacion) {}
