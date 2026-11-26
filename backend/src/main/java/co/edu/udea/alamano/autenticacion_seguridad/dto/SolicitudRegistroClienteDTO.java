package co.edu.udea.alamano.autenticacion_seguridad.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record SolicitudRegistroClienteDTO(
        @NotBlank(message = "El nombre es obligatorio.")
        @Size(max = 120, message = "El nombre no puede superar 120 caracteres.")
        String nombre,

        @NotBlank(message = "El correo es obligatorio.")
        String correo,

        @NotBlank(message = "El celular es obligatorio.")
        String celular,

        @NotBlank(message = "La contraseña es obligatoria.")
        String contrasena,

        @NotBlank(message = "La ubicación es obligatoria.")
        @Size(max = 255, message = "La ubicación no puede superar 255 caracteres.")
        String ubicacion,

        @NotNull(message = "La latitud es obligatoria.")
        Double latitud,

        @NotNull(message = "La longitud es obligatoria.")
        Double longitud,

        @AssertTrue(message = "Debes aceptar los términos y condiciones.")
        boolean aceptaTerminos
) {
}
