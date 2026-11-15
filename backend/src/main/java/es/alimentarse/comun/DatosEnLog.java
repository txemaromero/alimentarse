package es.alimentarse.comun;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.jspecify.annotations.Nullable;

/**
 * Lo que se escribe en el log sin datos personales (D-089): los emails, enmascarados, y de los errores de la base de
 * datos solo el nombre de la restricción, porque MySQL copia en el mensaje el valor que choca (un email, por ejemplo).
 */
public final class DatosEnLog {

    private static final String SEPARADORES = "\\s<>@'\"(),;:";
    private static final Pattern EMAIL = Pattern.compile("[^" + SEPARADORES + "]+@[^" + SEPARADORES + "]+");

    /** «… for key 'usuarios.UK_…'», «CONSTRAINT `FK_…` FOREIGN KEY …» y «Check constraint 'CK_…' is violated». */
    private static final List<Pattern> RESTRICCIONES = List.of(
            Pattern.compile("for key '([^']+)'"),
            Pattern.compile("CONSTRAINT `([^`]+)`"),
            Pattern.compile("constraint '([^']+)'", Pattern.CASE_INSENSITIVE));

    private DatosEnLog() {}

    /** «l***@toca.local»: la inicial y el dominio, que bastan para reconocer un fallo del servidor de correo. */
    public static String email(@Nullable String email) {
        if (email == null || email.isBlank()) {
            return "(sin email)";
        }
        String limpio = email.strip();
        int arroba = limpio.lastIndexOf('@');
        if (arroba <= 0) {
            return "***";
        }
        return limpio.charAt(0) + "***" + limpio.substring(arroba);
    }

    /** El texto con cada email enmascarado: los mensajes de los servidores de correo suelen repetir la dirección. */
    public static String sinEmails(@Nullable String texto) {
        if (texto == null) {
            return "";
        }
        Matcher email = EMAIL.matcher(texto);
        StringBuilder resultado = new StringBuilder();
        while (email.find()) {
            email.appendReplacement(resultado, Matcher.quoteReplacement(email(email.group())));
        }
        email.appendTail(resultado);
        return resultado.toString();
    }

    /** De un error de MySQL, solo la restricción («usuarios.UK_USUARIOS_EMAIL»), nunca el valor que choca. */
    public static String restriccion(@Nullable String mensaje) {
        if (mensaje != null) {
            for (Pattern patron : RESTRICCIONES) {
                Matcher restriccion = patron.matcher(mensaje);
                if (restriccion.find()) {
                    return restriccion.group(1);
                }
            }
        }
        return "(restricción desconocida)";
    }
}
