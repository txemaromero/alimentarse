package es.alimentarse.push;

import java.time.Instant;

/** Una comida planificada cuyo recordatorio aún no se ha enviado (D-078). */
public record ComidaPorAvisar(long id, String usuario, int receta, String titulo, Instant fecha) {}
