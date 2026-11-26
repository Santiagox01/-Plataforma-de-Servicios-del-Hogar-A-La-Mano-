package co.edu.udea.alamano.moderacion_reportes.dto;

import co.edu.udea.alamano.moderacion_reportes.dominio.MotivoReporte;

public record CrearReporteDTO(
        Long idSolicitud,
        MotivoReporte motivo,
        String descripcion
) {
}
