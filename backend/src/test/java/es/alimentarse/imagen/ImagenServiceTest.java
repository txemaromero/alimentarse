package es.alimentarse.imagen;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import es.alimentarse.comun.error.ExcepcionApi;
import es.alimentarse.support.ImagenesDePrueba;
import es.alimentarse.support.PropiedadesDePrueba;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionSynchronizationUtils;

class ImagenServiceTest {

    @TempDir
    Path directorio;

    private ImagenService imagenes;

    @BeforeEach
    void crearServicio() {
        imagenes = new ImagenService(PropiedadesDePrueba.conUploadDir(directorio.toString()));
    }

    @AfterEach
    void limpiarTransaccion() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    void guardaCadaFormatoComoJpegConUnNombreUuid() throws Exception {
        String png = imagenes.guardar(ImagenesDePrueba.parteFoto("foto.jpg", ImagenesDePrueba.png()));
        String jpeg = imagenes.guardar(ImagenesDePrueba.parteFoto("foto", ImagenesDePrueba.jpeg()));
        String webp = imagenes.guardar(ImagenesDePrueba.parteFoto("foto.png", ImagenesDePrueba.webp()));

        assertThat(png).matches("[0-9a-f-]{36}\\.jpg");
        assertThat(jpeg).endsWith(".jpg");
        assertThat(webp).endsWith(".jpg");
        assertThat(TipoImagen.detectar(Files.readAllBytes(directorio.resolve(png))))
                .contains(TipoImagen.JPEG);
    }

    @ParameterizedTest
    @ValueSource(strings = {"GIF89a imagen gif", "esto es texto", ""})
    void rechazaLoQueNoEsJpegPngNiWebp(String contenido) {
        MockMultipartFile fichero = ImagenesDePrueba.parteFoto("falsa.png", contenido.getBytes(StandardCharsets.UTF_8));

        assertThatThrownBy(() -> imagenes.guardar(fichero)).isInstanceOfSatisfying(ExcepcionApi.class, e -> {
            assertThat(e.getEstado()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(e.getErrores()).containsEntry("imagen", ImagenService.TIPO_NO_ADMITIDO);
        });
    }

    @Test
    void rechazaUnFicheroConFirmaDeImagenQueNoSePuedeLeer() {
        byte[] falso = new byte[64];
        System.arraycopy(ImagenesDePrueba.png(), 0, falso, 0, 8);

        assertThatThrownBy(() -> imagenes.guardar(ImagenesDePrueba.parteFoto("falsa.png", falso)))
                .isInstanceOfSatisfying(ExcepcionApi.class, e -> {
                    assertThat(e.getEstado()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(e.getErrores()).containsEntry("imagen", ImagenService.NO_LEGIBLE);
                });
    }

    @Test
    void rechazaImagenesDeMasDeCincoMegas() {
        byte[] grande = new byte[(int) ImagenService.TAMANO_MAXIMO + 1];
        System.arraycopy(ImagenesDePrueba.png(), 0, grande, 0, 8);

        assertThatThrownBy(() -> imagenes.guardar(ImagenesDePrueba.parteFoto("grande.png", grande)))
                .isInstanceOfSatisfying(
                        ExcepcionApi.class,
                        e -> assertThat(e.getErrores()).containsEntry("imagen", ImagenService.DEMASIADO_GRANDE));
    }

    @Test
    void siLaTransaccionSeDeshaceLaImagenNuevaSeBorra() {
        TransactionSynchronizationManager.initSynchronization();
        String nombre = imagenes.guardar(ImagenesDePrueba.parteFoto("foto.png", ImagenesDePrueba.png()));
        assertThat(directorio.resolve(nombre)).exists();

        TransactionSynchronizationUtils.invokeAfterCompletion(
                TransactionSynchronizationManager.getSynchronizations(), TransactionSynchronization.STATUS_ROLLED_BACK);

        assertThat(directorio.resolve(nombre)).doesNotExist();
    }

    @Test
    void laImagenSustituidaSoloSeBorraSiLaTransaccionSeConfirma() throws Exception {
        String anterior = imagenes.guardar(ImagenesDePrueba.parteFoto("a.png", ImagenesDePrueba.png()));
        TransactionSynchronizationManager.initSynchronization();

        imagenes.borrarTrasCommit(anterior);
        assertThat(directorio.resolve(anterior)).exists();
        TransactionSynchronizationUtils.invokeAfterCompletion(
                TransactionSynchronizationManager.getSynchronizations(), TransactionSynchronization.STATUS_COMMITTED);

        assertThat(directorio.resolve(anterior)).doesNotExist();
    }

    @Test
    void sirveLasSubidasYLasImagenesDeDemostracion() {
        String nombre = imagenes.guardar(ImagenesDePrueba.parteFoto("foto.png", ImagenesDePrueba.png()));

        ImagenService.Imagen subida = imagenes.cargar(nombre);
        ImagenService.Imagen demo = imagenes.cargar("demo-tortilla.jpg");

        assertThat(subida.recurso()).isInstanceOf(FileSystemResource.class);
        assertThat(subida.tipo()).isEqualTo(MediaType.IMAGE_JPEG);
        assertThat(demo.recurso()).isInstanceOf(ClassPathResource.class);
        assertThat(demo.tipo()).isEqualTo(MediaType.IMAGE_JPEG);
    }

    @Test
    void generaCadaVersionLaPrimeraVezYLuegoLaReutiliza() throws Exception {
        String nombre = imagenes.guardar(ImagenesDePrueba.parteFoto("foto.jpg", ImagenesDePrueba.jpeg(1500, 1000)));
        Path variante = directorio.resolve("variantes").resolve(nombre.replace(".jpg", "-400.jpg"));
        assertThat(variante).doesNotExist();

        ImagenService.Imagen reducida = imagenes.cargar(nombre, 400);

        assertThat(variante).exists();
        assertThat(ImageIO.read(variante.toFile()).getWidth()).isEqualTo(400);
        assertThat(reducida.tipo()).isEqualTo(MediaType.IMAGE_JPEG);
        long fecha = Files.getLastModifiedTime(variante).toMillis();
        assertThat(imagenes.cargar(nombre, 400).recurso().getFile()).isEqualTo(variante.toFile());
        assertThat(Files.getLastModifiedTime(variante).toMillis()).isEqualTo(fecha);
    }

    @Test
    void sinAnchoSeSirveLaImagenGuardada() {
        String nombre = imagenes.guardar(ImagenesDePrueba.parteFoto("foto.jpg", ImagenesDePrueba.jpeg(1500, 1000)));

        assertThat(imagenes.cargar(nombre, null).recurso())
                .isEqualTo(new FileSystemResource(directorio.resolve(nombre)));
    }

    @Test
    void lasImagenesDeDemostracionTambienTienenVersiones() throws Exception {
        ImagenService.Imagen reducida = imagenes.cargar("demo-tortilla.jpg", 400);

        assertThat(reducida.recurso()).isInstanceOf(FileSystemResource.class);
        assertThat(ImageIO.read(reducida.recurso().getFile()).getWidth()).isEqualTo(400);
    }

    @Test
    void siLaImagenYaEsEstrechaSeSirveTalCual() {
        String nombre = imagenes.guardar(ImagenesDePrueba.parteFoto("foto.jpg", ImagenesDePrueba.jpeg(300, 200)));

        assertThat(imagenes.cargar(nombre, 800).recurso())
                .isEqualTo(new FileSystemResource(directorio.resolve(nombre)));
        assertThat(directorio.resolve("variantes")).doesNotExist();
    }

    @Test
    void lasImagenesAntiguasEnPngTienenVersionesEnJpeg() throws Exception {
        ByteArrayOutputStream png = new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(900, 600, BufferedImage.TYPE_INT_RGB), "png", png);
        Files.write(directorio.resolve("antigua.png"), png.toByteArray());

        ImagenService.Imagen reducida = imagenes.cargar("antigua.png", 400);

        assertThat(reducida.tipo()).isEqualTo(MediaType.IMAGE_JPEG);
        assertThat(directorio.resolve("variantes").resolve("antigua-400.jpg")).exists();
    }

    @Test
    void siNoSePuedeGenerarLaVersionSeSirveLaOriginal() throws Exception {
        Files.write(directorio.resolve("rota.jpg"), new byte[] {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 0, 0});

        assertThat(imagenes.cargar("rota.jpg", 400).recurso().getFilename()).isEqualTo("rota.jpg");
    }

    @Test
    void soloSePuedenPedirLosAnchosPrevistos() {
        assertThatThrownBy(() -> imagenes.cargar("demo-tortilla.jpg", 401))
                .isInstanceOfSatisfying(ExcepcionApi.class, e -> {
                    assertThat(e.getEstado()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(e.getErrores()).containsKey("ancho");
                });
    }

    @Test
    void alBorrarUnaImagenSeBorranSusVersiones() {
        String nombre = imagenes.guardar(ImagenesDePrueba.parteFoto("foto.jpg", ImagenesDePrueba.jpeg(1500, 1000)));
        imagenes.cargar(nombre, 400);
        imagenes.cargar(nombre, 800);

        imagenes.borrar(nombre);

        assertThat(directorio.resolve(nombre)).doesNotExist();
        assertThat(directorio.resolve("variantes")).isEmptyDirectory();
    }

    @ParameterizedTest
    @ValueSource(strings = {"../application.yml", "..\\secreto.jpg", "no-existe.jpg", "imagen.gif", "sin-extension"})
    void losNombresNoValidosOInexistentesDan404(String nombre) throws Exception {
        Files.writeString(directorio.resolve("imagen.gif"), "x");

        assertThatThrownBy(() -> imagenes.cargar(nombre))
                .isInstanceOfSatisfying(
                        ExcepcionApi.class, e -> assertThat(e.getEstado()).isEqualTo(HttpStatus.NOT_FOUND));
    }

    @Test
    void laUrlPublicaDeUnaImagen() {
        assertThat(ImagenService.url("abc.png")).isEqualTo("/api/imagenes/abc.png");
        assertThat(ImagenService.url(null)).isNull();
    }
}
