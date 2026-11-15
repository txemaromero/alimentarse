package es.alimentarse.imagen;

import es.alimentarse.comun.error.ExcepcionApi;
import es.alimentarse.config.AppProperties;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;
import javax.imageio.ImageIO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

/**
 * Imágenes de las recetas en {@code UPLOAD_DIR} (6.9): solo JPEG, PNG o WebP de hasta 5 MB, con nombre UUID. Se
 * guardan recodificadas a JPEG y se sirven también reducidas, en {@code UPLOAD_DIR/variantes} (D-069). Las de las
 * recetas demo salen de {@code classpath:demo/imagenes/} (D-034).
 */
@Service
public class ImagenService {

    static final long TAMANO_MAXIMO = 5L * 1024 * 1024;
    static final String TIPO_NO_ADMITIDO = "La imagen debe ser JPEG, PNG o WebP";
    static final String DEMASIADO_GRANDE = "La imagen no puede superar 5 MB";
    static final String NO_LEGIBLE = "No se ha podido leer la imagen: puede estar dañada o ser demasiado grande";
    static final String ANCHO_NO_DISPONIBLE = "Ese ancho no está disponible";

    /** Anchos que se pueden pedir. Son pocos y fijos: se generan una sola vez por imagen. */
    static final Set<Integer> ANCHOS = Set.of(400, 800, 1200);

    static final String URL_BASE = "/api/imagenes/";

    private static final Logger log = LoggerFactory.getLogger(ImagenService.class);
    private static final Pattern NOMBRE_VALIDO = Pattern.compile("[A-Za-z0-9_-]+\\.(jpg|png|webp)");
    private static final String DEMO = "demo/imagenes/";

    private static final String EXTENSION_JPEG = "." + TipoImagen.JPEG.extension();

    private final Path directorio;
    private final Path variantes;

    public ImagenService(AppProperties propiedades) {
        this.directorio = Path.of(propiedades.uploadDir()).toAbsolutePath().normalize();
        this.variantes = directorio.resolve("variantes");
        // Sin ficheros temporales y con los lectores de las librerías también dentro del JAR (WebP)
        ImageIO.setUseCache(false);
        ImageIO.scanForPlugins();
    }

    /** Imagen lista para servir. */
    public record Imagen(Resource recurso, MediaType tipo) {}

    /** URL pública de una imagen, o {@code null}. */
    public static String url(String nombre) {
        return nombre == null ? null : URL_BASE + nombre;
    }

    /**
     * Valida la imagen, la recodifica a JPEG y devuelve su nombre. Si hay una transacción en curso y se deshace, el
     * fichero se borra.
     */
    public String guardar(MultipartFile fichero) {
        return guardar(fichero, "imagen");
    }

    /** Igual, con los errores en el campo {@code campo} (las fotos de una galería: «fotos[2]», D-083). */
    public String guardar(MultipartFile fichero, String campo) {
        if (fichero.getSize() > TAMANO_MAXIMO) {
            throw errorImagen(campo, DEMASIADO_GRANDE);
        }
        byte[] contenido;
        try {
            contenido = fichero.getBytes();
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo leer la imagen subida", e);
        }
        TipoImagen.detectar(contenido).orElseThrow(() -> errorImagen(campo, TIPO_NO_ADMITIDO));
        byte[] normalizada;
        try {
            normalizada = ProcesadorImagenes.normalizar(contenido);
        } catch (ProcesadorImagenes.ImagenNoValida e) {
            log.info("Imagen rechazada: {}", e.getMessage());
            throw errorImagen(campo, NO_LEGIBLE);
        }
        String nombre = UUID.randomUUID() + EXTENSION_JPEG;
        try {
            Files.createDirectories(directorio);
            Files.write(directorio.resolve(nombre), normalizada);
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo guardar la imagen en " + directorio, e);
        }
        alTerminarLaTransaccion(false, nombre);
        return nombre;
    }

    /** Borra una imagen sustituida, pero solo cuando la transacción se confirma (o ya, si no hay transacción). */
    public void borrarTrasCommit(String nombre) {
        if (nombre != null) {
            alTerminarLaTransaccion(true, nombre);
        }
    }

    /**
     * La imagen reducida a {@code ancho}; sin ancho, la imagen guardada. Cada versión se genera la primera vez que se
     * pide, así que también valen las imágenes anteriores a D-069 y las de demostración. Si no se puede generar o la
     * imagen ya es más estrecha, se sirve la guardada.
     */
    public Imagen cargar(String nombre, Integer ancho) {
        if (ancho != null && !ANCHOS.contains(ancho)) {
            throw ExcepcionApi.peticionIncorrecta(
                    "VALIDACION", ANCHO_NO_DISPONIBLE + ".", Map.of("ancho", ANCHO_NO_DISPONIBLE));
        }
        Imagen original = cargar(nombre);
        return ancho == null ? original : variante(nombre, original, ancho);
    }

    private Imagen variante(String nombre, Imagen original, int ancho) {
        Path fichero = rutaVariante(nombre, ancho);
        if (Files.isRegularFile(fichero)) {
            return new Imagen(new FileSystemResource(fichero), TipoImagen.JPEG.mediaType());
        }
        try {
            Optional<byte[]> reducida =
                    ProcesadorImagenes.reducir(original.recurso().getContentAsByteArray(), ancho);
            if (reducida.isEmpty()) {
                return original;
            }
            escribirAtomico(fichero, reducida.get());
            return new Imagen(new FileSystemResource(fichero), TipoImagen.JPEG.mediaType());
        } catch (IOException | ProcesadorImagenes.ImagenNoValida e) {
            log.warn("No se pudo generar la versión de {} px de {}", ancho, nombre, e);
            return original;
        }
    }

    /** Dos peticiones a la vez generan lo mismo; nadie llega a leer un fichero a medio escribir. */
    private void escribirAtomico(Path destino, byte[] contenido) throws IOException {
        Files.createDirectories(variantes);
        Path temporal = Files.createTempFile(variantes, "tmp", ".parte");
        try {
            Files.write(temporal, contenido);
            Files.move(temporal, destino, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } finally {
            Files.deleteIfExists(temporal);
        }
    }

    private Path rutaVariante(String nombre, int ancho) {
        return variantes.resolve(nombre.substring(0, nombre.lastIndexOf('.')) + "-" + ancho + EXTENSION_JPEG);
    }

    public Imagen cargar(String nombre) {
        if (nombre == null || !NOMBRE_VALIDO.matcher(nombre).matches()) {
            throw noEncontrada();
        }
        MediaType tipo = TipoImagen.porExtension(nombre)
                .orElseThrow(ImagenService::noEncontrada)
                .mediaType();
        Path fichero = directorio.resolve(nombre);
        if (Files.isRegularFile(fichero)) {
            return new Imagen(new FileSystemResource(fichero), tipo);
        }
        ClassPathResource demo = new ClassPathResource(DEMO + nombre);
        if (demo.exists()) {
            return new Imagen(demo, tipo);
        }
        throw noEncontrada();
    }

    private void alTerminarLaTransaccion(boolean borrarSiConfirma, String nombre) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            if (borrarSiConfirma) {
                borrar(nombre);
            }
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int estado) {
                boolean confirmada = estado == STATUS_COMMITTED;
                if (confirmada == borrarSiConfirma) {
                    borrar(nombre);
                }
            }
        });
    }

    void borrar(String nombre) {
        if (!NOMBRE_VALIDO.matcher(nombre).matches()) {
            return;
        }
        try {
            Files.deleteIfExists(directorio.resolve(nombre));
            for (int ancho : ANCHOS) {
                Files.deleteIfExists(rutaVariante(nombre, ancho));
            }
        } catch (IOException e) {
            log.warn("No se pudo borrar la imagen {}", nombre, e);
        }
    }

    private static ExcepcionApi errorImagen(String campo, String mensaje) {
        return ExcepcionApi.peticionIncorrecta("VALIDACION", mensaje + ".", Map.of(campo, mensaje));
    }

    private static ExcepcionApi noEncontrada() {
        return ExcepcionApi.noEncontrado("La imagen no existe.");
    }
}
