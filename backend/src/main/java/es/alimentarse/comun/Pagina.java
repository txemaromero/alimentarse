package es.alimentarse.comun;

import java.util.List;
import java.util.function.Function;

/**
 * Página de resultados con el formato común de la API (sección 7).
 *
 * @param pagina número de página, empezando en 0 (D-026)
 * @param tamano elementos por página solicitados
 * @param totalElementos elementos que cumplen el filtro, en todas las páginas
 */
public record Pagina<T>(List<T> contenido, int pagina, int tamano, long totalElementos, int totalPaginas) {

    public Pagina {
        contenido = List.copyOf(contenido);
    }

    public static <T> Pagina<T> de(List<T> contenido, int pagina, int tamano, long totalElementos) {
        int totalPaginas = (int) ((totalElementos + tamano - 1) / tamano);
        return new Pagina<>(contenido, pagina, tamano, totalElementos, totalPaginas);
    }

    /** La misma página con cada elemento transformado. */
    public <R> Pagina<R> map(Function<? super T, ? extends R> transformar) {
        return new Pagina<>(
                contenido.stream().<R>map(transformar).toList(), pagina, tamano, totalElementos, totalPaginas);
    }

    /** Primer elemento de la página, para {@code LIMIT … OFFSET}. */
    public static long desplazamiento(int pagina, int tamano) {
        return (long) pagina * tamano;
    }
}
