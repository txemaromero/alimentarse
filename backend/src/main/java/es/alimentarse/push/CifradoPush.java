package es.alimentarse.push;

import static java.nio.charset.StandardCharsets.US_ASCII;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.security.AlgorithmParameters;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.SecureRandom;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.security.spec.ECFieldFp;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.ECParameterSpec;
import java.security.spec.ECPoint;
import java.security.spec.ECPrivateKeySpec;
import java.security.spec.ECPublicKeySpec;
import java.util.Arrays;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.KeyAgreement;
import javax.crypto.Mac;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * Cifrado del contenido de un aviso push (D-078): RFC 8291, con la codificación {@code aes128gcm} de RFC 8188. Solo
 * usa la criptografía del JDK. También convierte las claves P-256 al formato de Web Push: la pública como punto sin
 * comprimir (65 bytes) y la privada como escalar (32 bytes), en Base64 URL sin relleno.
 */
final class CifradoPush {

    /** Tamaño de registro (RFC 8188): un aviso cabe siempre en un solo registro. */
    static final int TAMANO_REGISTRO = 4096;

    /** Lo que cabe en un registro, descontando el delimitador de relleno y la etiqueta de AES-GCM. */
    static final int MAXIMO_MENSAJE = TAMANO_REGISTRO - 1 - 16;

    private static final ECParameterSpec P256 = parametrosP256();
    private static final SecureRandom AZAR = new SecureRandom();
    private static final byte[] INFO_CLAVE = "WebPush: info\0".getBytes(US_ASCII);
    private static final byte[] INFO_CEK = "Content-Encoding: aes128gcm\0".getBytes(US_ASCII);
    private static final byte[] INFO_NONCE = "Content-Encoding: nonce\0".getBytes(US_ASCII);
    private static final byte DELIMITADOR_ULTIMO_REGISTRO = 2;

    private CifradoPush() {}

    /** Cifra el mensaje para un navegador, con un par de claves efímero y una sal nuevos. */
    static byte[] cifrar(byte[] mensaje, byte[] clavePublicaNavegador, byte[] secretoAuth) {
        try {
            KeyPairGenerator generador = KeyPairGenerator.getInstance("EC");
            generador.initialize(P256, AZAR);
            byte[] sal = new byte[16];
            AZAR.nextBytes(sal);
            return cifrar(mensaje, clavePublicaNavegador, secretoAuth, generador.generateKeyPair(), sal);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("No se ha podido cifrar el aviso", e);
        }
    }

    /**
     * Cifrado determinista, con la clave efímera y la sal dadas (los tests lo comprueban con el ejemplo del apéndice A
     * de RFC 8291). Devuelve el cuerpo completo: sal, tamaño de registro, clave pública efímera y texto cifrado.
     */
    static byte[] cifrar(
            byte[] mensaje, byte[] clavePublicaNavegador, byte[] secretoAuth, KeyPair efimera, byte[] sal) {
        if (mensaje.length > MAXIMO_MENSAJE) {
            throw new IllegalArgumentException("El aviso no cabe en un registro: " + mensaje.length + " bytes");
        }
        try {
            byte[] publicaServidor = puntoSinComprimir((ECPublicKey) efimera.getPublic());
            KeyAgreement acuerdo = KeyAgreement.getInstance("ECDH");
            acuerdo.init(efimera.getPrivate());
            acuerdo.doPhase(clavePublica(clavePublicaNavegador), true);
            byte[] secretoEcdh = acuerdo.generateSecret();

            byte[] prkClave = hkdfExtraer(secretoAuth, secretoEcdh);
            byte[] ikm = hkdfExpandir(prkClave, concatenar(INFO_CLAVE, clavePublicaNavegador, publicaServidor), 32);
            byte[] prk = hkdfExtraer(sal, ikm);
            byte[] cek = hkdfExpandir(prk, INFO_CEK, 16);
            byte[] nonce = hkdfExpandir(prk, INFO_NONCE, 12);

            Cipher aes = Cipher.getInstance("AES/GCM/NoPadding");
            aes.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(cek, "AES"), new GCMParameterSpec(128, nonce));
            byte[] cifrado = aes.doFinal(concatenar(mensaje, new byte[] {DELIMITADOR_ULTIMO_REGISTRO}));

            return ByteBuffer.allocate(sal.length + 4 + 1 + publicaServidor.length + cifrado.length)
                    .put(sal)
                    .putInt(TAMANO_REGISTRO)
                    .put((byte) publicaServidor.length)
                    .put(publicaServidor)
                    .put(cifrado)
                    .array();
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("No se ha podido cifrar el aviso", e);
        }
    }

    /**
     * Si los 65 bytes son un punto sin comprimir de la curva P-256 (y² = x³ + ax + b mod p). Lo que manda el navegador
     * se comprueba al suscribirse: con un punto que no es de la curva, el cifrado fallaría en cada aviso (D-089).
     */
    static boolean puntoP256(byte[] punto) {
        if (punto.length != 65 || punto[0] != 4) {
            return false;
        }
        BigInteger p = ((ECFieldFp) P256.getCurve().getField()).getP();
        BigInteger x = new BigInteger(1, Arrays.copyOfRange(punto, 1, 33));
        BigInteger y = new BigInteger(1, Arrays.copyOfRange(punto, 33, 65));
        if (x.compareTo(p) >= 0 || y.compareTo(p) >= 0) {
            return false;
        }
        BigInteger derecha = x.pow(3)
                .add(P256.getCurve().getA().multiply(x))
                .add(P256.getCurve().getB())
                .mod(p);
        return y.modPow(BigInteger.TWO, p).equals(derecha);
    }

    /** Clave pública P-256 a partir de su punto sin comprimir (0x04 ‖ X ‖ Y). */
    static ECPublicKey clavePublica(byte[] punto) throws GeneralSecurityException {
        if (punto.length != 65 || punto[0] != 4) {
            throw new IllegalArgumentException("La clave pública no es un punto P-256 sin comprimir");
        }
        ECPoint w = new ECPoint(
                new BigInteger(1, Arrays.copyOfRange(punto, 1, 33)),
                new BigInteger(1, Arrays.copyOfRange(punto, 33, 65)));
        return (ECPublicKey) KeyFactory.getInstance("EC").generatePublic(new ECPublicKeySpec(w, P256));
    }

    /** Clave privada P-256 a partir de su escalar de 32 bytes. */
    static ECPrivateKey clavePrivada(byte[] escalar) throws GeneralSecurityException {
        if (escalar.length != 32) {
            throw new IllegalArgumentException("La clave privada no tiene 32 bytes");
        }
        return (ECPrivateKey)
                KeyFactory.getInstance("EC").generatePrivate(new ECPrivateKeySpec(new BigInteger(1, escalar), P256));
    }

    static byte[] puntoSinComprimir(ECPublicKey clave) {
        return concatenar(
                new byte[] {4},
                fijo32(clave.getW().getAffineX()),
                fijo32(clave.getW().getAffineY()));
    }

    static byte[] base64Url(String texto) {
        return Base64.getUrlDecoder().decode(texto.strip());
    }

    static String base64Url(byte[] datos) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(datos);
    }

    private static byte[] hkdfExtraer(byte[] sal, byte[] ikm) throws GeneralSecurityException {
        Mac hmac = Mac.getInstance("HmacSHA256");
        hmac.init(new SecretKeySpec(sal, "HmacSHA256"));
        return hmac.doFinal(ikm);
    }

    /** HKDF-Expand para un solo bloque: como mucho 32 bytes, que es todo lo que hace falta aquí. */
    private static byte[] hkdfExpandir(byte[] prk, byte[] info, int longitud) throws GeneralSecurityException {
        Mac hmac = Mac.getInstance("HmacSHA256");
        hmac.init(new SecretKeySpec(prk, "HmacSHA256"));
        hmac.update(info);
        hmac.update((byte) 1);
        return Arrays.copyOf(hmac.doFinal(), longitud);
    }

    /** Un entero sin signo en 32 bytes exactos, con ceros a la izquierda. */
    private static byte[] fijo32(BigInteger numero) {
        byte[] bytes = numero.toByteArray();
        byte[] resultado = new byte[32];
        int copiar = Math.min(bytes.length, 32);
        System.arraycopy(bytes, bytes.length - copiar, resultado, 32 - copiar, copiar);
        return resultado;
    }

    private static byte[] concatenar(byte[]... partes) {
        ByteBuffer total = ByteBuffer.allocate(
                Arrays.stream(partes).mapToInt(p -> p.length).sum());
        for (byte[] parte : partes) {
            total.put(parte);
        }
        return total.array();
    }

    private static ECParameterSpec parametrosP256() {
        try {
            AlgorithmParameters parametros = AlgorithmParameters.getInstance("EC");
            parametros.init(new ECGenParameterSpec("secp256r1"));
            return parametros.getParameterSpec(ECParameterSpec.class);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("El JDK no admite la curva P-256", e);
        }
    }
}
