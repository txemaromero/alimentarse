package es.alimentarse.receta;

import es.alimentarse.comun.Pagina;
import es.alimentarse.comun.Texto;
import es.alimentarse.comun.error.ExcepcionApi;
import java.time.Clock;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Valoraciones de las recetas (D-077): una nota de 1 a 5 y un comentario opcional por usuario y receta. Conviven con
 * los «me gusta», que son los favoritos de cada uno.
 */
@Service
public class ValoracionService {

    static final String PROPIA_RECETA = "No puedes valorar tu propia receta.";

    /** Caracteres de control, salvo el salto de línea. */
    private static final Pattern CONTROL = Pattern.compile("[\\p{Cc}&&[^\\n]]");

    private final RecetaService recetas;
    private final ValoracionMapper valoraciones;
    private final Clock reloj;

    public ValoracionService(RecetaService recetas, ValoracionMapper valoraciones, Clock reloj) {
        this.recetas = recetas;
        this.valoraciones = valoraciones;
        this.reloj = reloj;
    }

    /**
     * Las de una receta que el usuario puede ver, de la más reciente a la más antigua. Las ocultas por la
     * administración solo se listan a los administradores, para poder volver a mostrarlas.
     */
    @Transactional(readOnly = true)
    public Pagina<ValoracionDto> listar(int receta, String yo, boolean admin, int pagina, int tamano) {
        recetas.visible(receta, yo, admin);
        long total = valoraciones.contar(receta, admin);
        long desplazamiento = Pagina.desplazamiento(pagina, tamano);
        List<ValoracionDto> contenido =
                desplazamiento >= total ? List.of() : valoraciones.deReceta(receta, admin, desplazamiento, tamano);
        return Pagina.de(contenido, pagina, tamano, total);
    }

    /**
     * Crea o cambia la propia, nunca en una receta propia. No hace falta comprobar que esté publicada: quien no es su
     * autor solo ve una receta si está publicada y no oculta (D-032, D-063).
     */
    @Transactional
    public ResumenValoracionesDto valorar(int id, ValoracionRequest datos, String yo) {
        RecetaBase receta = recetas.visible(id, yo, false);
        if (receta.esDe(yo)) {
            throw ExcepcionApi.prohibido("PROPIA_RECETA", PROPIA_RECETA);
        }
        valoraciones.guardar(
                yo,
                id,
                datos.nota(),
                comentario(datos.comentario()),
                reloj.instant().truncatedTo(ChronoUnit.SECONDS));
        return resumen(id, yo);
    }

    /** Idempotente. */
    @Transactional
    public ResumenValoracionesDto borrar(int id, String yo) {
        recetas.visible(id, yo, false);
        valoraciones.borrar(id, yo);
        return resumen(id, yo);
    }

    /**
     * Moderación: la oculta (deja de verse y de contar en la media) o la vuelve a mostrar. Quien llama ya ha comprobado
     * que es administrador.
     */
    @Transactional
    public ValoracionDto ocultar(long id, boolean oculta) {
        if (valoraciones.actualizarOculta(id, oculta) == 0) {
            throw ExcepcionApi.noEncontrado("La valoración no existe.");
        }
        return valoraciones.buscar(id).orElseThrow();
    }

    private ResumenValoracionesDto resumen(int receta, String yo) {
        NotaMedia media = valoraciones.notaMedia(receta);
        return new ResumenValoracionesDto(
                media.media(), media.total(), valoraciones.deUsuario(receta, yo).orElse(null));
    }

    /**
     * Texto plano: sin caracteres de control salvo los saltos de línea, y sin espacios a los lados; {@code null} si
     * queda vacío. Se muestra siempre como texto, nunca como HTML.
     */
    static String comentario(String texto) {
        if (texto == null) {
            return null;
        }
        String unificado = texto.replace("\r\n", "\n").replace('\r', '\n');
        return Texto.opcional(CONTROL.matcher(unificado).replaceAll(""));
    }
}
