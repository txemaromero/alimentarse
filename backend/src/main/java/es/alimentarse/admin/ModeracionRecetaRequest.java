package es.alimentarse.admin;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.jspecify.annotations.Nullable;

/**
 * Ocultar una receta o volver a mostrarla (D-063).
 *
 * @param motivo obligatorio al ocultarla; lo verá el autor
 */
public record ModeracionRecetaRequest(
        @NotNull(message = "Indica si la receta queda oculta")
        Boolean oculta,

        @Size(max = 500, message = "El motivo no puede superar 500 caracteres") @Nullable
        String motivo) {}
