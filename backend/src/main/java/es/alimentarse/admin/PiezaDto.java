package es.alimentarse.admin;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

/**
 * Peso aproximado de una pieza de un ingrediente («unidad», «diente», «lata»), en gramos de parte comestible, para
 * convertir cantidades como «2 latas» (D-079, D-081).
 *
 * @param pieza una sola palabra, en singular; se guarda en minúsculas
 */
public record PiezaDto(
        @NotBlank(message = "Escribe la pieza")
        @Size(max = 30, message = "Como mucho 30 caracteres")
        @Pattern(regexp = "\\s*\\p{L}+\\s*", message = "Una sola palabra, sin números")
        String pieza,

        @NotNull(message = "Escribe el peso")
        @DecimalMin(value = "0", inclusive = false, message = "Debe pesar más de 0 g")
        @DecimalMax(value = "10000", message = "Como mucho 10.000 g")
        @Digits(integer = 5, fraction = 1, message = "Como mucho un decimal")
        BigDecimal gramos) {}
