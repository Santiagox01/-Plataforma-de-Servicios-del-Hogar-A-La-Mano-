package co.edu.udea.alamano.gestion_servicios.dto;

import jakarta.validation.constraints.Size;

public record RechazarSolicitudDTO(
        @Size(max = 500, message = "El motivo no puede superar 500 caracteres.")
        String motivo
) {
}
