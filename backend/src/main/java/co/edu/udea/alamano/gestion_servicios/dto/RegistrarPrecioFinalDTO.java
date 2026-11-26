package co.edu.udea.alamano.gestion_servicios.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record RegistrarPrecioFinalDTO(
        @NotNull(message = "El precio final es obligatorio.")
        @DecimalMin(value = "0.01", message = "El precio final debe ser mayor a cero.")
        BigDecimal precioFinal
) {
}
