package co.edu.udea.alamano.moderacion_reportes.dto;

import co.edu.udea.alamano.moderacion_reportes.dominio.MotivoReporte;

public record MotivoReporteDTO(
        MotivoReporte codigo,
        String etiqueta
) {
}
