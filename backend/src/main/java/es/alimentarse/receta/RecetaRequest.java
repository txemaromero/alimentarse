package es.alimentarse.receta;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * Alta o edición de una receta: la parte {@code receta} (JSON) de la petición multipart (D-036, D-037).
 *
 * @param instrucciones HTML del editor; se sanea antes de guardarlo
 * @param tipo id del tipo de receta
 * @param tiempo minutos
 * @param fotos la galería completa, en orden (D-083): la primera es la principal; {@code null} deja la que tenga
 * @param video enlace {@code https} a un vídeo de YouTube o Vimeo (D-083); {@code null} o vacío, ninguno
 */
public record RecetaRequest(
        @NotBlank(message = "El título es obligatorio") @Size(max = 100, message = "Máximo 100 caracteres")
        String titulo,

        @NotBlank(message = "Las instrucciones son obligatorias")
        @Size(max = 20_000, message = "Las instrucciones no pueden superar los 20.000 caracteres")
        String instrucciones,

        @NotNull(message = "Elige el tipo de receta") Integer tipo,

        @NotNull(message = "Indica el tiempo de preparación")
        @Min(value = 1, message = "El tiempo debe estar entre 1 y 1440 minutos")
        @Max(value = 1440, message = "El tiempo debe estar entre 1 y 1440 minutos")
        Integer tiempo,

        @NotNull(message = "Indica los comensales")
        @Min(value = 1, message = "Los comensales deben estar entre 1 y 100")
        @Max(value = 100, message = "Los comensales deben estar entre 1 y 100")
        Integer comensales,

        @NotNull(message = "Elige la dificultad")
        @Schema(allowableValues = {"Fácil", "Media", "Difícil"})
        @Pattern(regexp = "Fácil|Media|Difícil", message = "La dificultad debe ser Fácil, Media o Difícil")
        String dificultad,

        // Un solo @Size: con @NotEmpty además, una lista vacía daría dos mensajes y el OpenAPI perdería el mínimo
        @NotNull(message = "Añade al menos un ingrediente")
        @Size(min = 1, max = 50, message = "Añade entre 1 y 50 ingredientes")
        List<@Valid @NotNull IngredienteCantidad> ingredientes,

        @Nullable @Size(max = 6, message = "Como mucho 6 fotos")
        List<@Valid @NotNull(message = "Falta la foto") FotoRequest> fotos,

        @Nullable @Size(max = 500, message = "Como mucho 500 caracteres")
        String video) {}
