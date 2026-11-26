package co.edu.udea.alamano.autenticacion_seguridad.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RechazoVerificacionDTO(
        @NotBlank(message = "El motivo del rechazo es obligatorio.")
        @Size(max = 500, message = "El motivo no puede superar 500 caracteres.")
        String motivo
) {
}
