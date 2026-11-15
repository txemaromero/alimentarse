package es.alimentarse.comun;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;

/**
 * Tokens de un solo uso para los enlaces de los correos (recuperar la contraseña, confirmar el email): 256 bits
 * aleatorios en Base64 URL. En la base de datos solo se guarda su SHA-256 (6.9).
 */
public final class TokenSeguro {

    private static final int BYTES = 32;
    private static final SecureRandom ALEATORIO = new SecureRandom();

    private TokenSeguro() {}

    public static String nuevo() {
        byte[] bytes = new byte[BYTES];
        ALEATORIO.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /** SHA-256 en hexadecimal. */
    public static String sha256(String texto) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(texto.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 no disponible", e);
        }
    }
}
