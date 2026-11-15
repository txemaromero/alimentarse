package es.alimentarse.push;

import java.time.Instant;

/**
 * Fila de {@code suscripcion_push}: un navegador de un usuario que recibe avisos (D-078).
 *
 * @param endpoint URL del servicio push para ese navegador; quien la conoce puede enviarle avisos
 * @param p256dh clave pública del navegador, en Base64 URL
 * @param auth secreto de autenticación del navegador, en Base64 URL
 * @param zona zona horaria del navegador, para escribir la hora en el aviso
 */
public record SuscripcionPush(
        long id, String usuario, String endpoint, String p256dh, String auth, String zona, Instant fecha) {}
