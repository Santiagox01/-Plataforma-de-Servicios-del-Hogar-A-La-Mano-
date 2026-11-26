package co.edu.udea.alamano.gestion_servicios.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

public record CrearResenaDTO(
        @Min(value = 1, message = "La calificación mínima es 1 estrella.")
        @Max(value = 5, message = "La calificación máxima es 5 estrellas.")
        int estrellas,

        @Size(max = 600, message = "El comentario no puede superar 600 caracteres.")
        String comentario
) {
}
