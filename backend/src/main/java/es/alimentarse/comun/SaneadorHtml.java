package es.alimentarse.comun;

import java.util.regex.Pattern;
import org.owasp.html.PolicyFactory;
import org.owasp.html.Sanitizers;
import org.springframework.stereotype.Component;

/**
 * Sanea el HTML que escriben los usuarios (instrucciones de las recetas, 6.9) con una lista blanca de OWASP: formato
 * básico, bloques (párrafos, títulos, listas, citas) y enlaces http/https/mailto con {@code rel="nofollow"}. Quita
 * scripts, atributos de eventos, estilos e imágenes (D-035).
 */
@Component
public class SaneadorHtml {

    private static final PolicyFactory POLITICA =
            Sanitizers.FORMATTING.and(Sanitizers.BLOCKS).and(Sanitizers.LINKS);
    private static final Pattern ETIQUETA = Pattern.compile("<[^>]*>");
    private static final Pattern ESPACIO_HTML = Pattern.compile("&nbsp;|&#160;|&#xa0;", Pattern.CASE_INSENSITIVE);

    public String sanear(String html) {
        return html == null ? "" : POLITICA.sanitize(html);
    }

    /** ¿Queda algún texto visible, aparte de etiquetas y espacios? */
    public static boolean tieneTexto(String html) {
        if (html == null) {
            return false;
        }
        String texto =
                ESPACIO_HTML.matcher(ETIQUETA.matcher(html).replaceAll(" ")).replaceAll(" ");
        return !texto.isBlank();
    }
}
