package es.alimentarse.imagen;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import es.alimentarse.support.ImagenesDePrueba;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.zip.CRC32;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;

class ProcesadorImagenesTest {

    @Test
    void todoSeGuardaComoJpeg() throws Exception {
        for (byte[] original :
                new byte[][] {ImagenesDePrueba.png(), ImagenesDePrueba.jpeg(), ImagenesDePrueba.webp()}) {
            byte[] resultado = ProcesadorImagenes.normalizar(original);

            assertThat(TipoImagen.detectar(resultado)).contains(TipoImagen.JPEG);
            assertThat(leer(resultado).getWidth()).isEqualTo(1);
        }
    }

    @Test
    void limitaElLadoMayorSinDeformarLaImagen() throws Exception {
        BufferedImage horizontal = leer(ProcesadorImagenes.normalizar(ImagenesDePrueba.jpeg(3200, 1600)));
        BufferedImage vertical = leer(ProcesadorImagenes.normalizar(ImagenesDePrueba.jpeg(1000, 2000)));

        assertThat(horizontal.getWidth()).isEqualTo(1600);
        assertThat(horizontal.getHeight()).isEqualTo(800);
        assertThat(vertical.getWidth()).isEqualTo(800);
        assertThat(vertical.getHeight()).isEqualTo(1600);
    }

    @Test
    void noAgrandaLasImagenesPequenas() throws Exception {
        BufferedImage imagen = leer(ProcesadorImagenes.normalizar(ImagenesDePrueba.jpeg(300, 200)));

        assertThat(imagen.getWidth()).isEqualTo(300);
        assertThat(imagen.getHeight()).isEqualTo(200);
    }

    @Test
    void respetaLaOrientacionDeLaFotoYEliminaSusMetadatos() throws Exception {
        byte[] conExif = conOrientacion(ImagenesDePrueba.jpeg(400, 200), 6);

        byte[] resultado = ProcesadorImagenes.normalizar(conExif);

        // Orientación 6: la foto se hizo girada 90 grados
        assertThat(leer(resultado).getWidth()).isEqualTo(200);
        assertThat(leer(resultado).getHeight()).isEqualTo(400);
        assertThat(new String(resultado, StandardCharsets.ISO_8859_1)).doesNotContain("Exif");
    }

    @Test
    void loTransparenteSeVeBlanco() throws Exception {
        BufferedImage transparente = new BufferedImage(20, 20, BufferedImage.TYPE_INT_ARGB);
        ByteArrayOutputStream png = new ByteArrayOutputStream();
        ImageIO.write(transparente, "png", png);

        BufferedImage resultado = leer(ProcesadorImagenes.normalizar(png.toByteArray()));

        assertThat(new Color(resultado.getRGB(10, 10)).getRed()).isGreaterThan(250);
    }

    @Test
    void rechazaLoQueTieneFirmaDeImagenPeroNoLoEs() {
        byte[] falso = new byte[64];
        System.arraycopy(ImagenesDePrueba.png(), 0, falso, 0, 8);

        assertThatThrownBy(() -> ProcesadorImagenes.normalizar(falso))
                .isInstanceOf(ProcesadorImagenes.ImagenNoValida.class);
    }

    @Test
    void rechazaLasImagenesConDemasiadosPixelesSinDescomprimirlas() throws Exception {
        assertThatThrownBy(() -> ProcesadorImagenes.normalizar(pngConCabecera(20_000, 20_000)))
                .isInstanceOf(ProcesadorImagenes.ImagenNoValida.class)
                .hasRootCauseMessage("Demasiados píxeles: Dimensiones[ancho=20000, alto=20000]");
        // D-089: como mucho 25 megapíxeles, unos 100 MB en memoria
        assertThatThrownBy(() -> ProcesadorImagenes.normalizar(pngConCabecera(5_001, 5_000)))
                .isInstanceOf(ProcesadorImagenes.ImagenNoValida.class)
                .hasRootCauseMessage("Demasiados píxeles: Dimensiones[ancho=5001, alto=5000]");
    }

    @Test
    void soloDescomprimeDosImagenesALaVez() throws Exception {
        ProcesadorImagenes.TURNOS.acquire(ProcesadorImagenes.A_LA_VEZ);
        CompletableFuture<byte[]> tercera;
        try {
            tercera = CompletableFuture.supplyAsync(() -> {
                try {
                    return ProcesadorImagenes.normalizar(ImagenesDePrueba.jpeg(300, 200));
                } catch (ProcesadorImagenes.ImagenNoValida e) {
                    throw new IllegalStateException(e);
                }
            });
            long limite = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
            while (!ProcesadorImagenes.TURNOS.hasQueuedThreads() && System.nanoTime() < limite) {
                Thread.onSpinWait();
            }
            assertThat(ProcesadorImagenes.TURNOS.hasQueuedThreads()).isTrue();
            assertThat(tercera).isNotDone();
        } finally {
            ProcesadorImagenes.TURNOS.release(ProcesadorImagenes.A_LA_VEZ);
        }

        assertThat(TipoImagen.detectar(tercera.get(10, TimeUnit.SECONDS))).contains(TipoImagen.JPEG);
        assertThat(ProcesadorImagenes.TURNOS.availablePermits()).isEqualTo(ProcesadorImagenes.A_LA_VEZ);
    }

    @Test
    void devuelveElTurnoAunqueNoPuedaDescomprimirLaImagen() {
        // La cabecera es correcta, pero no hay píxeles: falla al descomprimir, con el turno ya tomado
        assertThatThrownBy(() -> ProcesadorImagenes.normalizar(pngConCabecera(10, 10)))
                .isInstanceOf(ProcesadorImagenes.ImagenNoValida.class);

        assertThat(ProcesadorImagenes.TURNOS.availablePermits()).isEqualTo(ProcesadorImagenes.A_LA_VEZ);
    }

    @Test
    void reduceAlAnchoPedido() throws Exception {
        BufferedImage reducida = leer(ProcesadorImagenes.reducir(ImagenesDePrueba.jpeg(1200, 900), 400)
                .orElseThrow());

        assertThat(reducida.getWidth()).isEqualTo(400);
        assertThat(reducida.getHeight()).isEqualTo(300);
    }

    @Test
    void noReduceLoQueYaEsMasEstrecho() throws Exception {
        assertThat(ProcesadorImagenes.reducir(ImagenesDePrueba.jpeg(400, 300), 400))
                .isEmpty();
        assertThat(ProcesadorImagenes.reducir(ImagenesDePrueba.jpeg(300, 300), 400))
                .isEmpty();
    }

    private static BufferedImage leer(byte[] imagen) throws Exception {
        return ImageIO.read(new ByteArrayInputStream(imagen));
    }

    /** Inserta tras el SOI un segmento APP1 con un EXIF mínimo que solo lleva la orientación. */
    private static byte[] conOrientacion(byte[] jpeg, int orientacion) {
        ByteBuffer exif = ByteBuffer.allocate(2 + 2 + 6 + 8 + 2 + 12 + 4);
        exif.putShort((short) 0xFFE1).putShort((short) (exif.capacity() - 2));
        exif.put("Exif\0\0".getBytes(StandardCharsets.US_ASCII));
        exif.put("MM".getBytes(StandardCharsets.US_ASCII)).putShort((short) 42).putInt(8);
        exif.putShort((short) 1);
        exif.putShort((short) 0x0112)
                .putShort((short) 3)
                .putInt(1)
                .putShort((short) orientacion)
                .putShort((short) 0);
        exif.putInt(0);
        ByteBuffer resultado = ByteBuffer.allocate(jpeg.length + exif.capacity());
        resultado.put(jpeg, 0, 2).put(exif.array()).put(jpeg, 2, jpeg.length - 2);
        return resultado.array();
    }

    /** PNG que solo tiene firma y cabecera: dice medir lo indicado, sin píxeles. */
    private static byte[] pngConCabecera(int ancho, int alto) {
        ByteBuffer ihdr = ByteBuffer.allocate(4 + 13);
        ihdr.put("IHDR".getBytes(StandardCharsets.US_ASCII));
        ihdr.putInt(ancho)
                .putInt(alto)
                .put((byte) 8)
                .put((byte) 2)
                .put((byte) 0)
                .put((byte) 0)
                .put((byte) 0);
        CRC32 crc = new CRC32();
        crc.update(ihdr.array());
        ByteBuffer png = ByteBuffer.allocate(8 + 4 + 17 + 4);
        png.put(new byte[] {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A});
        png.putInt(13).put(ihdr.array()).putInt((int) crc.getValue());
        return png.array();
    }
}
