package es.alimentarse.admin;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.jspecify.annotations.Nullable;

/**
 * Alta o edición de un grupo de alimentos (D-065).
 *
 * @param racionesSemana {@code null} = sin límite
 */
public record GrupoRequest(
        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 100, message = "El nombre no puede superar 100 caracteres")
        String nombre,

        @Min(value = 1, message = "Debe ser al menos 1 ración a la semana")
        @Max(value = 99, message = "Como mucho, 99 raciones a la semana")
        @Nullable
        Integer racionesSemana,

        @NotNull(message = "Indica si es un grupo básico") Boolean basico) {}
