package co.edu.udea.alamano.autenticacion_seguridad.dto;

import co.edu.udea.alamano.autenticacion_seguridad.dominio.EstadoVerificacion;

import java.time.OffsetDateTime;

public record ResultadoVerificacionDTO(
        Long idProfesional,
        String nombreProfesional,
        EstadoVerificacion estadoVerificacion,
        String motivoRechazo,
        OffsetDateTime fechaVerificacion,
        boolean disponible
) {
}
