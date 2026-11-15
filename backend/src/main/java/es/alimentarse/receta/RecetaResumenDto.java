package es.alimentarse.receta;

import es.alimentarse.catalogo.TipoRecetaDto;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import org.jspecify.annotations.Nullable;

/**
 * Datos de la tarjeta de receta (listados).
 *
 * @param autor id del usuario que la creó
 * @param imagen URL de la imagen ({@code /api/imagenes/…}) o {@code null}
 * @param meGusta si el usuario actual le ha dado «me gusta»
 * @param nota media de sus valoraciones visibles, con un decimal, o {@code null} si no tiene (D-077)
 * @param valoraciones cuántas valoraciones visibles tiene
 * @param eliminada la eliminó su autor después de que alguien la consumiera: solo aparece en el histórico de consumos,
 *     sin imagen ni enlace (D-061)
 * @param oculta la ha ocultado un administrador: solo la ven su autor y los administradores (D-063)
 */
public record RecetaResumenDto(
        int id,
        String titulo,
        @Nullable String autor,
        TipoRecetaDto tipo,
        @Nullable String imagen,
        int tiempo,
        int comensales,

        @Schema(allowableValues = {"Fácil", "Media", "Difícil"})
        String dificultad,

        long likes,
        boolean meGusta,
        @Nullable Double nota,
        long valoraciones,
        boolean publicada,
        Instant fechaCreacion,
        boolean eliminada,
        boolean oculta) {}
