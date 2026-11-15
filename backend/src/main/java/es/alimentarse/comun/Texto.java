package es.alimentarse.comun;

/**
 * Utilidades para el texto que escribe el usuario. Los patrones de búsqueda se usan con {@code LIKE … ESCAPE '\\'}; la
 * colación de la base de datos ya ignora mayúsculas y acentos (D-027).
 */
public final class Texto {

    private Texto() {}

    /** Texto recortado, o {@code null} si está vacío o es {@code null}. */
    public static String opcional(String texto) {
        return texto == null || texto.isBlank() ? null : texto.strip();
    }

    /** Patrón «contiene», o {@code null} si no hay texto (sin filtro). */
    public static String patronContiene(String texto) {
        String limpio = opcional(texto);
        return limpio == null ? null : "%" + escaparComodines(limpio) + "%";
    }

    /** Patrón «empieza por», o {@code null} si no hay texto (sin filtro). */
    public static String patronEmpiezaPor(String texto) {
        String limpio = opcional(texto);
        return limpio == null ? null : escaparComodines(limpio) + "%";
    }

    /** Hace que {@code \}, {@code %} y {@code _} se busquen literalmente. */
    static String escaparComodines(String texto) {
        return texto.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}
