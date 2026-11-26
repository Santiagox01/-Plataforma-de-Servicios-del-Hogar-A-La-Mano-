package co.edu.udea.alamano.gestion_servicios.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record EnviarMensajeDTO(
        @NotBlank(message = "El contenido del mensaje es obligatorio.")
        @Size(max = 1000, message = "El mensaje no puede superar 1000 caracteres.")
        String contenido
) {
}
