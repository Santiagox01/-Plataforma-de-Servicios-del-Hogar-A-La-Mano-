package co.edu.udea.alamano.moderacion_reportes.dto;

import jakarta.validation.constraints.NotBlank;

public record CrearCasoSoporteDTO(
        @NotBlank(message = "El asunto del caso es obligatorio.")
        String asunto,
        @NotBlank(message = "La descripción del problema es obligatoria.")
        String descripcion,
        Long idSolicitud
) {
}
