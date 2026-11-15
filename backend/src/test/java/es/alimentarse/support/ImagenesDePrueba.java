package es.alimentarse.support;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Base64;
import javax.imageio.ImageIO;
import org.springframework.mock.web.MockMultipartFile;

/** Ficheros de imagen reales para los tests: el servidor los decodifica al recibirlos (D-069). */
public final class ImagenesDePrueba {

    private ImagenesDePrueba() {}

    /** WebP de 1x1 con pérdida. ImageIO no sabe escribir WebP, así que va ya codificado. */
    private static final String WEBP_BASE64 = "UklGRiIAAABXRUJQVlA4IBYAAAAwAQCdASoBAAEADsD+JaQAA3AAAAAA";

    public static byte[] png() {
        return codificar("png", 1, 1);
    }

    public static byte[] jpeg() {
        return codificar("jpg", 1, 1);
    }

    public static byte[] jpeg(int ancho, int alto) {
        return codificar("jpg", ancho, alto);
    }

    public static byte[] webp() {
        return Base64.getDecoder().decode(WEBP_BASE64);
    }

    /** Una foto de la galería de una receta: la parte {@code fotos} de la petición multipart (D-083). */
    public static MockMultipartFile parteFoto(String nombreFichero, byte[] contenido) {
        return new MockMultipartFile("fotos", nombreFichero, "application/octet-stream", contenido);
    }

    private static byte[] codificar(String formato, int ancho, int alto) {
        try {
            BufferedImage imagen = new BufferedImage(ancho, alto, BufferedImage.TYPE_INT_RGB);
            imagen.setRGB(0, 0, 0x3A7D44);
            ByteArrayOutputStream salida = new ByteArrayOutputStream();
            ImageIO.write(imagen, formato, salida);
            return salida.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
