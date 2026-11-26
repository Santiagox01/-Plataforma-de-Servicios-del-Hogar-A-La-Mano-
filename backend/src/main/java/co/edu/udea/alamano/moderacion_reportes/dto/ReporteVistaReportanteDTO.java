package co.edu.udea.alamano.moderacion_reportes.dto;

import co.edu.udea.alamano.moderacion_reportes.dominio.AccionModeracion;
import co.edu.udea.alamano.moderacion_reportes.dominio.EstadoReporte;
import co.edu.udea.alamano.moderacion_reportes.dominio.MotivoReporte;

import java.time.OffsetDateTime;

public record ReporteVistaReportanteDTO(
        Long idReporte,
        Long idSolicitud,
        String nombreCategoria,
        Long idReportado,
        String nombreReportado,
        MotivoReporte motivo,
        String etiquetaMotivo,
        String descripcion,
        boolean esPrioritario,
        EstadoReporte estadoReporte,
        AccionModeracion accionTomada,
        String justificacionResolucion,
        OffsetDateTime fechaReporte,
        OffsetDateTime fechaResolucion
) {
}
