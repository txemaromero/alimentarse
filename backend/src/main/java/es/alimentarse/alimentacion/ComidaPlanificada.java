package es.alimentarse.alimentacion;

import java.time.Instant;

/** Fila de {@code usuario_receta_planificada}. */
record ComidaPlanificada(long id, String usuario, int receta, Instant fecha) {

    boolean esDe(String yo) {
        return usuario.equals(yo);
    }
}
