package es.alimentarse.seguridad;

import java.nio.charset.StandardCharsets;
import org.springframework.core.env.Environment;

/**
 * Cómo se cifran las contraseñas (D-090). bcrypt solo admite 72 bytes, y en UTF-8 la ñ y las letras con tilde ocupan
 * 2 y un emoji, 4: {@code @Size(max = 72)}, que cuenta caracteres, deja pasar contraseñas que bcrypt se niega a cifrar.
 */
public final class CifradoPassword {

    /**
     * El coste de bcrypt, 2^12 vueltas, y el mínimo fuera de los tests. Un hash con un coste menor se rehace al entrar
     * ({@link UsuarioDetallesService}).
     */
    public static final int COSTE = 12;

    public static final int MAXIMO_BYTES = 72;

    private CifradoPassword() {}

    /**
     * El coste configurado ({@code app.contrasenas.coste-bcrypt}). Solo el perfil {@code test} puede bajarlo: fuera de
     * él, uno menor que {@link #COSTE} impide arrancar, para que nadie debilite así las contraseñas de producción.
     */
    public static int coste(int configurado, Environment entorno) {
        if (configurado < COSTE && !entorno.matchesProfiles("test")) {
            throw new IllegalStateException("app.contrasenas.coste-bcrypt no puede bajar de " + COSTE
                    + " fuera de los tests: es la seguridad de las contraseñas guardadas (D-090).");
        }
        return configurado;
    }

    /** Si bcrypt puede cifrarla entera. */
    public static boolean cabe(String password) {
        return password.getBytes(StandardCharsets.UTF_8).length <= MAXIMO_BYTES;
    }
}
