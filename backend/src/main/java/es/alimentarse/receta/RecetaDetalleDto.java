package es.alimentarse.receta;

import es.alimentarse.catalogo.GrupoDto;
import es.alimentarse.catalogo.TipoRecetaDto;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * Detalle de una receta (7).
 *
 * @param instrucciones HTML ya saneado
 * @param imagen URL de la foto principal o {@code null}
 * @param fotos la galería, en orden; la primera es la principal (D-083)
 * @param video enlace a un vídeo de la receta en YouTube o Vimeo, o {@code null} (D-083)
 * @param grupos grupos de alimentos que contiene, sin repetir y por nombre
 * @param maxSemanal veces a la semana como máximo (6.1); {@code null} = sin límite
 * @param esAutor si el usuario actual la creó (puede editarla y publicarla)
 * @param oculta la ha ocultado un administrador (D-063); mientras lo esté, no se puede publicar
 * @param motivoOculta el motivo que dio el administrador, o {@code null}
 * @param nota media de sus valoraciones visibles, con un decimal, o {@code null} si no tiene (D-077)
 * @param valoraciones cuántas valoraciones visibles tiene
 * @param miValoracion la del usuario actual, o {@code null}
 */
public record RecetaDetalleDto(
        int id,
        String titulo,
        String instrucciones,
        @Nullable String autor,
        TipoRecetaDto tipo,
        @Nullable String imagen,
        List<FotoDto> fotos,
        @Nullable String video,
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
        List<IngredienteRecetaDto> ingredientes,
        List<GrupoDto> grupos,
        @Nullable Integer maxSemanal,
        boolean esAutor,
        boolean oculta,
        @Nullable String motivoOculta,
        @Nullable ValoracionDto miValoracion) {}
