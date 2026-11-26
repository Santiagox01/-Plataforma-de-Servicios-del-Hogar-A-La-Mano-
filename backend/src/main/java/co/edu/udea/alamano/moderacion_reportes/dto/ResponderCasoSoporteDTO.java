package co.edu.udea.alamano.moderacion_reportes.dto;

import jakarta.validation.constraints.NotBlank;

public record ResponderCasoSoporteDTO(
        @NotBlank(message = "La respuesta de soporte es obligatoria.")
        String respuesta,
        Boolean cerrar
) {
}
