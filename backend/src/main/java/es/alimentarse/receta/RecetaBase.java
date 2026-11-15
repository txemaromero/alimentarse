package es.alimentarse.receta;

import es.alimentarse.catalogo.TipoRecetaDto;
import java.time.Instant;

/**
 * Fila de una receta tal como la lee el mapper para el detalle y las comprobaciones de permisos.
 *
 * @param oculta la ha ocultado un administrador (D-063)
 * @param motivoOculta por qué, o {@code null}
 * @param imagen nombre del fichero (no la URL), o {@code null}
 */
record RecetaBase(
        int id,
        String titulo,
        String instrucciones,
        String autor,
        TipoRecetaDto tipo,
        boolean publicada,
        boolean eliminada,
        boolean oculta,
        String motivoOculta,
        String imagen,
        int tiempo,
        int comensales,
        String dificultad,
        long likes,
        boolean meGusta,
        Double nota,
        long valoraciones,
        Instant fechaCreacion) {

    boolean esDe(String usuario) {
        return autor != null && autor.equalsIgnoreCase(usuario);
    }

    /** La pueden ver todos: publicada y no oculta por la administración. */
    boolean publica() {
        return publicada && !oculta && !eliminada;
    }

    /** Las no publicadas solo las ve su autor (6.6); las eliminadas, nadie (D-061). */
    boolean visiblePara(String usuario) {
        return visiblePara(usuario, false);
    }

    /** Las ocultas solo las ven su autor y los administradores (D-063). */
    boolean visiblePara(String usuario, boolean admin) {
        return !eliminada && (esDe(usuario) || publica() || (admin && oculta));
    }
}
