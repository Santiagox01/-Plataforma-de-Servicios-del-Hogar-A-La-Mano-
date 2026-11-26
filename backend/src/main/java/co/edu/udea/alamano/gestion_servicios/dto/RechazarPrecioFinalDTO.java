package co.edu.udea.alamano.gestion_servicios.dto;

import jakarta.validation.constraints.Size;

public record RechazarPrecioFinalDTO(
        @Size(max = 500, message = "La observación no puede superar 500 caracteres.")
        String motivo
) {
}
