package co.edu.udea.alamano.autenticacion_seguridad.dto;

import jakarta.validation.constraints.NotNull;

public record DisponibilidadDTO(
        @NotNull(message = "El estado de disponibilidad es obligatorio.")
        Boolean disponible
) {
}
