package es.alimentarse.push;

import static java.nio.charset.StandardCharsets.US_ASCII;

import java.nio.ByteBuffer;
import java.util.Arrays;
import javax.crypto.Cipher;
import javax.crypto.KeyAgreement;
import javax.crypto.Mac;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/** El lado del navegador de RFC 8291, solo para los tests: descifra un aviso con la clave privada del navegador. */
final class DescifradoNavegador {

    private DescifradoNavegador() {}

    static byte[] descifrar(byte[] cuerpo, String privadaNavegador, String publicaNavegador, String auth)
            throws Exception {
        ByteBuffer lector = ByteBuffer.wrap(cuerpo);
        byte[] sal = new byte[16];
        lector.get(sal);
        lector.getInt();
        byte[] publicaServidor = new byte[lector.get()];
        lector.get(publicaServidor);
        byte[] cifrado = new byte[lector.remaining()];
        lector.get(cifrado);

        KeyAgreement acuerdo = KeyAgreement.getInstance("ECDH");
        acuerdo.init(CifradoPush.clavePrivada(CifradoPush.base64Url(privadaNavegador)));
        acuerdo.doPhase(CifradoPush.clavePublica(publicaServidor), true);
        byte[] publicaUa = CifradoPush.base64Url(publicaNavegador);
        byte[] info = ByteBuffer.allocate(14 + publicaUa.length + publicaServidor.length)
                .put("WebPush: info\0".getBytes(US_ASCII))
                .put(publicaUa)
                .put(publicaServidor)
                .array();
        byte[] ikm = expandir(extraer(CifradoPush.base64Url(auth), acuerdo.generateSecret()), info, 32);
        byte[] prk = extraer(sal, ikm);
        byte[] cek = expandir(prk, "Content-Encoding: aes128gcm\0".getBytes(US_ASCII), 16);
        byte[] nonce = expandir(prk, "Content-Encoding: nonce\0".getBytes(US_ASCII), 12);

        Cipher aes = Cipher.getInstance("AES/GCM/NoPadding");
        aes.init(Cipher.DECRYPT_MODE, new SecretKeySpec(cek, "AES"), new GCMParameterSpec(128, nonce));
        byte[] claro = aes.doFinal(cifrado);
        if (claro[claro.length - 1] != 2) {
            throw new IllegalStateException("Falta el delimitador del último registro");
        }
        return Arrays.copyOf(claro, claro.length - 1);
    }

    private static byte[] extraer(byte[] sal, byte[] ikm) throws Exception {
        Mac hmac = Mac.getInstance("HmacSHA256");
        hmac.init(new SecretKeySpec(sal, "HmacSHA256"));
        return hmac.doFinal(ikm);
    }

    private static byte[] expandir(byte[] prk, byte[] info, int longitud) throws Exception {
        Mac hmac = Mac.getInstance("HmacSHA256");
        hmac.init(new SecretKeySpec(prk, "HmacSHA256"));
        hmac.update(info);
        hmac.update((byte) 1);
        return Arrays.copyOf(hmac.doFinal(), longitud);
    }
}
