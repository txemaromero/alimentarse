package es.alimentarse.push;

import org.jspecify.annotations.Nullable;

/**
 * Clave pública VAPID para suscribirse a los avisos (D-078).
 *
 * @param clavePublica en Base64 URL, o {@code null} si este servidor no tiene avisos
 */
public record ClavePushDto(@Nullable String clavePublica) {}
