package es.alimentarse.ingrediente;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Alta o edición de un ingrediente. @param grupo id del grupo de alimentos */
public record IngredienteRequest(
        @NotBlank(message = "El nombre es obligatorio") @Size(max = 100, message = "Máximo 100 caracteres")
        String nombre,

        @NotNull(message = "Elige un grupo") Integer grupo) {}
