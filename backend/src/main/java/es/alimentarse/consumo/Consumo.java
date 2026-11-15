package es.alimentarse.consumo;

import java.time.Instant;

/** Fila de {@code usuario_receta_consumida}: el usuario comió la receta en esa fecha. */
public record Consumo(long id, String usuario, int receta, Instant fecha) {

    boolean esDe(String otroUsuario) {
        return usuario.equalsIgnoreCase(otroUsuario);
    }
}
