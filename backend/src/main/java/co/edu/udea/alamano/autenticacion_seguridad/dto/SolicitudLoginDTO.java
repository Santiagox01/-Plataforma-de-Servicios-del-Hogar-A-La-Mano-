package co.edu.udea.alamano.autenticacion_seguridad.dto;

import jakarta.validation.constraints.NotBlank;

public record SolicitudLoginDTO(
        @NotBlank(message = "El correo es obligatorio.")
        String correo,

        @NotBlank(message = "La contraseña es obligatoria.")
        String contrasena
) {
}
