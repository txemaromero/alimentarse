package es.alimentarse.alimentacion;

import java.time.Instant;

/** Una comida planificada que entra en la lista de la compra (D-076). */
public record ComidaCompraDto(long id, Instant fecha, int receta, String titulo) {}
