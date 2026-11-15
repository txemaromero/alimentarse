package es.alimentarse.alimentacion;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.jspecify.annotations.Nullable;

/**
 * Total de un ingrediente en la lista de la compra (D-076). Las cantidades son texto libre («200 gramos», «1 unidad»,
 * «al gusto»), así que solo se suman si todas empiezan por un número y llevan la misma unidad, en singular o en plural:
 * «1 unidad» y «3 unidades» son «4 unidades». Si no, no hay total y la lista muestra lo que pide cada receta.
 */
final class CantidadesCompra {

    /** Un número (fracción mixta, fracción, o entero o decimal, con o sin separador de miles) y lo que le sigue. */
    private static final Pattern CANTIDAD = Pattern.compile("(\\d+ \\d+/\\d+|\\d+/\\d+|\\d+(?:[.,]\\d+)*) ?(.*)");

    /** «1.500» o «1.500,5»: el punto separa los miles. */
    private static final Pattern MILES = Pattern.compile("\\d{1,3}(?:\\.\\d{3})+(?:,\\d+)?");

    private static final Pattern ESPACIOS = Pattern.compile("\\s+");

    /** Números escritos con letras al principio de la cantidad: «media cebolla», «una lata», «medio kilo» (D-081). */
    private static final Map<String, String> NUMEROS_CON_LETRAS =
            Map.of("un", "1", "una", "1", "uno", "1", "medio", "1/2", "media", "1/2");

    private static final Pattern PRIMERA_PALABRA = Pattern.compile("^\\p{L}+(?= |$)");
    private static final Pattern MARCAS = Pattern.compile("\\p{M}+");
    private static final String VOCALES = "aeiou";
    private static final String CON_TILDE = "áéíóú";
    private static final Locale ES = Locale.forLanguageTag("es");

    private CantidadesCompra() {}

    /** Una cantidad leída: su número y su unidad, en minúsculas y con los espacios normalizados ("" si no tiene). */
    record Medida(BigDecimal numero, String unidad) {}

    /**
     * Lo que hay que comprar en total, o {@code null} si no se puede sumar. Si solo lo pide una comida, la cantidad tal
     * cual, aunque no sea un número.
     */
    static @Nullable String total(List<CantidadCompraDto> cantidades) {
        if (cantidades.size() == 1 && cantidades.getFirst().veces() == 1) {
            return cantidades.getFirst().cantidad().strip();
        }
        List<Medida> medidas = new ArrayList<>();
        BigDecimal suma = BigDecimal.ZERO;
        for (CantidadCompraDto cantidad : cantidades) {
            Optional<Medida> medida = leer(cantidad.cantidad());
            if (medida.isEmpty()
                    || (!medidas.isEmpty()
                            && !mismaUnidad(
                                    medidas.getFirst().unidad(), medida.get().unidad()))) {
                return null;
            }
            medidas.add(medida.get());
            suma = suma.add(medida.get().numero().multiply(BigDecimal.valueOf(cantidad.veces())));
        }
        if (medidas.isEmpty()) {
            return null;
        }
        String unidad = unidadPara(suma, medidas);
        return unidad.isEmpty() ? formatear(suma) : formatear(suma) + " " + unidad;
    }

    /** El número y la unidad de una cantidad, si empieza por un número y le sigue una palabra o nada. */
    static Optional<Medida> leer(String cantidad) {
        Matcher partes = CANTIDAD.matcher(
                conNumero(ESPACIOS.matcher(cantidad.strip()).replaceAll(" ").toLowerCase(ES)));
        if (!partes.matches()) {
            return Optional.empty();
        }
        String unidad = partes.group(2);
        // «2-3 dientes», «1 (grande)»: no es una unidad
        if (!unidad.isEmpty() && !Character.isLetter(unidad.charAt(0))) {
            return Optional.empty();
        }
        return numero(partes.group(1)).map(numero -> new Medida(numero, unidad));
    }

    /**
     * ¿Es la misma unidad? La primera palabra puede ir en singular o en plural, sin tener en cuenta las tildes
     * («limón», «limones»); el resto debe coincidir («dientes de ajo»).
     */
    static boolean mismaUnidad(String una, String otra) {
        String[] a = sinTildes(una).split(" ", 2);
        String[] b = sinTildes(otra).split(" ", 2);
        String restoA = a.length > 1 ? a[1] : "";
        String restoB = b.length > 1 ? b[1] : "";
        return restoA.equals(restoB) && (singularYPlural(a[0], b[0]) || singularYPlural(b[0], a[0]));
    }

    /** La primera palabra en plural: «unidad» → «unidades», «pizca» → «pizcas». Las abreviaturas («kg») no cambian. */
    static String pluralizar(String unidad) {
        if (unidad.isEmpty()) {
            return unidad;
        }
        String[] partes = unidad.split(" ", 2);
        String palabra = partes[0];
        String base = sinTildes(palabra);
        int largo = palabra.length();
        String plural;
        if (largo <= 2 || base.chars().noneMatch(letra -> VOCALES.indexOf(letra) >= 0)) {
            plural = palabra;
        } else if (VOCALES.indexOf(base.charAt(largo - 1)) >= 0) {
            plural = palabra + "s";
        } else if (palabra.endsWith("z")) {
            plural = palabra.substring(0, largo - 1) + "ces";
        } else if (CON_TILDE.indexOf(palabra.charAt(largo - 2)) >= 0) {
            // «limón» → «limones»: la tilde desaparece
            plural = palabra.substring(0, largo - 2) + base.charAt(largo - 2) + palabra.charAt(largo - 1) + "es";
        } else {
            plural = palabra + "es";
        }
        return partes.length > 1 ? plural + " " + partes[1] : plural;
    }

    private static boolean singularYPlural(String singular, String plural) {
        return plural.equals(singular)
                || plural.equals(singular + "s")
                || plural.equals(singular + "es")
                || (singular.endsWith("z") && plural.equals(singular.substring(0, singular.length() - 1) + "ces"));
    }

    /**
     * La unidad del total, en singular si es 1 y en plural si no. Si se han visto las dos formas, la más corta es el
     * singular; si solo una, es plural si su primera palabra acaba en «s», y si no, el plural se forma.
     */
    private static String unidadPara(BigDecimal suma, List<Medida> medidas) {
        List<String> formas = medidas.stream()
                .map(Medida::unidad)
                .distinct()
                .sorted(Comparator.comparingInt(String::length))
                .toList();
        String singular;
        String plural;
        if (formas.size() > 1) {
            singular = formas.getFirst();
            plural = formas.getLast();
        } else if (formas.getFirst().split(" ", 2)[0].endsWith("s")) {
            singular = null;
            plural = formas.getFirst();
        } else {
            singular = formas.getFirst();
            plural = pluralizar(singular);
        }
        if (suma.compareTo(BigDecimal.ONE) == 0 && singular != null) {
            return singular;
        }
        return plural;
    }

    /** «media cebolla» → «1/2 cebolla»; el resto, tal cual. */
    private static String conNumero(String cantidad) {
        Matcher palabra = PRIMERA_PALABRA.matcher(cantidad);
        if (palabra.find() && NUMEROS_CON_LETRAS.containsKey(palabra.group())) {
            return NUMEROS_CON_LETRAS.get(palabra.group()) + cantidad.substring(palabra.end());
        }
        return cantidad;
    }

    private static Optional<BigDecimal> numero(String texto) {
        try {
            if (texto.contains("/")) {
                String[] partes = texto.split(" ");
                BigDecimal entero = partes.length == 2 ? new BigDecimal(partes[0]) : BigDecimal.ZERO;
                String[] fraccion = partes[partes.length - 1].split("/");
                BigDecimal divisor = new BigDecimal(fraccion[1]);
                if (divisor.signum() == 0) {
                    return Optional.empty();
                }
                return Optional.of(entero.add(new BigDecimal(fraccion[0]).divide(divisor, 4, RoundingMode.HALF_UP)));
            }
            if (MILES.matcher(texto).matches()) {
                return Optional.of(new BigDecimal(texto.replace(".", "").replace(',', '.')));
            }
            // «1,5.2»: con más de un separador no se sabe qué número es
            if (texto.chars().filter(letra -> letra == '.' || letra == ',').count() > 1) {
                return Optional.empty();
            }
            return Optional.of(new BigDecimal(texto.replace(',', '.')));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }

    /** Con coma decimal, como mucho dos decimales y punto en los miles: «1,5», «1.500». */
    private static String formatear(BigDecimal numero) {
        DecimalFormat formato = new DecimalFormat("#,##0.##", DecimalFormatSymbols.getInstance(ES));
        formato.setRoundingMode(RoundingMode.HALF_UP);
        return formato.format(numero);
    }

    private static String sinTildes(String texto) {
        return MARCAS.matcher(Normalizer.normalize(texto, Normalizer.Form.NFD)).replaceAll("");
    }
}
