package co.edu.udea.alamano.cotizacion_tarificacion.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record ConfigurarTarifaDTO(
        @NotNull(message = "La categoría es obligatoria.")
        Long idCategoria,

        @NotNull(message = "La tarifa base es obligatoria.")
        @DecimalMin(value = "0.01", message = "La tarifa base debe ser mayor a cero.")
        BigDecimal tarifaBase,

        @NotNull(message = "El precio mínimo es obligatorio.")
        @DecimalMin(value = "0.01", message = "El precio mínimo debe ser mayor a cero.")
        BigDecimal precioMin,

        @NotNull(message = "El precio máximo es obligatorio.")
        @DecimalMin(value = "0.01", message = "El precio máximo debe ser mayor a cero.")
        BigDecimal precioMax
) {
}
