package co.edu.udea.alamano.moderacion_reportes.dto;

import co.edu.udea.alamano.moderacion_reportes.dominio.EstadoCasoSoporte;

import java.time.OffsetDateTime;

public record CasoSoporteDTO(
        Long idCaso,
        String numeroCaso,
        Long idUsuario,
        String nombreUsuario,
        Long idSolicitud,
        String asunto,
        String descripcion,
        EstadoCasoSoporte estadoCaso,
        String respuestaAdmin,
        Long idAdminRespondedor,
        OffsetDateTime fechaCreacion,
        OffsetDateTime fechaRespuesta
) {
}
