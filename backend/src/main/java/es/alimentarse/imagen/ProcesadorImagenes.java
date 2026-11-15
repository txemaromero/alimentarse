package es.alimentarse.imagen;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.Optional;
import java.util.concurrent.Semaphore;
import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.MemoryCacheImageOutputStream;
import net.coobird.thumbnailator.Thumbnails;

/**
 * Recodifica las imágenes a JPEG (D-069). Lo que se publica nunca es el fichero original: así se pierden sus metadatos,
 * como la ubicación GPS de una foto, y lo que no fuera realmente una imagen.
 */
final class ProcesadorImagenes {

    /** Lado mayor de la imagen que se guarda. */
    static final int LADO_MAXIMO = 1600;

    /**
     * Evita descomprimir en memoria una imagen pequeña en bytes pero enorme en píxeles: cada píxel ocupa hasta 4 bytes,
     * así que el máximo son unos 100 MB por imagen (D-089). Una foto real de 5 MB no llega.
     */
    static final long PIXELES_MAXIMOS = 25_000_000L;

    /** Cuántas imágenes se descomprimen a la vez, para que varias subidas simultáneas no agoten la memoria (D-089). */
    static final int A_LA_VEZ = 2;

    static final Semaphore TURNOS = new Semaphore(A_LA_VEZ, true);

    private static final float CALIDAD = 0.82f;

    private ProcesadorImagenes() {}

    /** Imagen no decodificable o demasiado grande en píxeles. */
    static final class ImagenNoValida extends Exception {
        private static final long serialVersionUID = 1L;

        ImagenNoValida(String mensaje, Throwable causa) {
            super(mensaje, causa);
        }
    }

    private record Dimensiones(int ancho, int alto) {
        long pixeles() {
            return (long) ancho * alto;
        }
    }

    /** Imagen a guardar: orientada según su EXIF, con el lado mayor limitado y en JPEG. */
    static byte[] normalizar(byte[] original) throws ImagenNoValida {
        try {
            Dimensiones dimensiones = dimensiones(original);
            int ladoMayor = Math.max(dimensiones.ancho(), dimensiones.alto());
            return escalarYCodificar(original, Math.min(1.0, (double) LADO_MAXIMO / ladoMayor));
        } catch (IOException | RuntimeException e) {
            throw new ImagenNoValida("No se ha podido leer la imagen", e);
        }
    }

    /** Versión de {@code ancho} píxeles; vacío si la imagen ya es igual o más estrecha. */
    static Optional<byte[]> reducir(byte[] imagen, int ancho) throws ImagenNoValida {
        try {
            Dimensiones dimensiones = dimensiones(imagen);
            if (dimensiones.ancho() <= ancho) {
                return Optional.empty();
            }
            return Optional.of(escalarYCodificar(imagen, (double) ancho / dimensiones.ancho()));
        } catch (IOException | RuntimeException e) {
            throw new ImagenNoValida("No se ha podido reducir la imagen", e);
        }
    }

    /** Lee solo la cabecera, antes de reservar memoria para los píxeles. */
    private static Dimensiones dimensiones(byte[] imagen) throws IOException {
        try (ImageInputStream entrada = ImageIO.createImageInputStream(new ByteArrayInputStream(imagen))) {
            Iterator<ImageReader> lectores = ImageIO.getImageReaders(entrada);
            if (!lectores.hasNext()) {
                throw new IOException("Formato sin lector");
            }
            ImageReader lector = lectores.next();
            try {
                lector.setInput(entrada);
                Dimensiones dimensiones = new Dimensiones(lector.getWidth(0), lector.getHeight(0));
                if (dimensiones.pixeles() > PIXELES_MAXIMOS) {
                    throw new IOException("Demasiados píxeles: " + dimensiones);
                }
                return dimensiones;
            } finally {
                lector.dispose();
            }
        }
    }

    /** Lo que ocupa memoria: descomprimir la imagen entera. Espera su turno si ya hay {@link #A_LA_VEZ} en marcha. */
    private static byte[] escalarYCodificar(byte[] imagen, double factor) throws IOException {
        TURNOS.acquireUninterruptibly();
        try {
            return codificar(escalar(imagen, factor));
        } finally {
            TURNOS.release();
        }
    }

    /** Thumbnailator aplica la orientación del EXIF. */
    private static BufferedImage escalar(byte[] imagen, double factor) throws IOException {
        return Thumbnails.of(new ByteArrayInputStream(imagen)).scale(factor).asBufferedImage();
    }

    private static byte[] codificar(BufferedImage imagen) throws IOException {
        BufferedImage sinTransparencia = sobreFondoBlanco(imagen);
        ImageWriter escritor = ImageIO.getImageWritersByFormatName("jpeg").next();
        ByteArrayOutputStream salida = new ByteArrayOutputStream();
        try (MemoryCacheImageOutputStream destino = new MemoryCacheImageOutputStream(salida)) {
            ImageWriteParam parametros = escritor.getDefaultWriteParam();
            parametros.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            parametros.setCompressionQuality(CALIDAD);
            parametros.setProgressiveMode(ImageWriteParam.MODE_DEFAULT);
            escritor.setOutput(destino);
            escritor.write(null, new IIOImage(sinTransparencia, null, null), parametros);
        } finally {
            escritor.dispose();
        }
        return salida.toByteArray();
    }

    /** JPEG no admite transparencia: lo transparente de un PNG o un WebP se ve blanco. */
    private static BufferedImage sobreFondoBlanco(BufferedImage imagen) {
        BufferedImage resultado = new BufferedImage(imagen.getWidth(), imagen.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D lienzo = resultado.createGraphics();
        try {
            lienzo.drawImage(imagen, 0, 0, Color.WHITE, null);
        } finally {
            lienzo.dispose();
        }
        return resultado;
    }
}
