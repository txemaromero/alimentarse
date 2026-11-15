package es.alimentarse.imagen;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Optional;
import org.springframework.http.MediaType;

/** Formatos de imagen admitidos (6.9). El tipo se deduce de los primeros bytes, no del nombre del fichero (D-034). */
public enum TipoImagen {
    JPEG("jpg", MediaType.IMAGE_JPEG),
    PNG("png", MediaType.IMAGE_PNG),
    WEBP("webp", MediaType.parseMediaType("image/webp"));

    private static final byte[] FIRMA_JPEG = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF};
    private static final byte[] FIRMA_PNG = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A};
    private static final byte[] RIFF = "RIFF".getBytes(StandardCharsets.US_ASCII);
    private static final byte[] WEBP_FIRMA = "WEBP".getBytes(StandardCharsets.US_ASCII);

    private final String extension;
    private final MediaType mediaType;

    TipoImagen(String extension, MediaType mediaType) {
        this.extension = extension;
        this.mediaType = mediaType;
    }

    public String extension() {
        return extension;
    }

    public MediaType mediaType() {
        return mediaType;
    }

    public static Optional<TipoImagen> detectar(byte[] contenido) {
        if (empiezaPor(contenido, 0, FIRMA_JPEG)) {
            return Optional.of(JPEG);
        }
        if (empiezaPor(contenido, 0, FIRMA_PNG)) {
            return Optional.of(PNG);
        }
        if (empiezaPor(contenido, 0, RIFF) && empiezaPor(contenido, 8, WEBP_FIRMA)) {
            return Optional.of(WEBP);
        }
        return Optional.empty();
    }

    public static Optional<TipoImagen> porExtension(String nombreFichero) {
        String extension = nombreFichero.substring(nombreFichero.lastIndexOf('.') + 1);
        return Arrays.stream(values())
                .filter(t -> t.extension.equals(extension))
                .findFirst();
    }

    private static boolean empiezaPor(byte[] contenido, int desde, byte[] firma) {
        return contenido.length >= desde + firma.length
                && Arrays.equals(contenido, desde, desde + firma.length, firma, 0, firma.length);
    }
}
