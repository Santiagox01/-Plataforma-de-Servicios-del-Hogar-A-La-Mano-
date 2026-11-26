package co.edu.udea.alamano.moderacion_reportes.dto;

import co.edu.udea.alamano.moderacion_reportes.dominio.AccionModeracion;
import co.edu.udea.alamano.moderacion_reportes.dominio.EstadoReporte;
import co.edu.udea.alamano.moderacion_reportes.dominio.MotivoReporte;

import java.time.OffsetDateTime;

/**
 * Vista del reporte dirigida al usuario reportado: omite por diseno cualquier identidad del reportante.
 */
public record ReporteVistaReportadoDTO(
        Long idReporte,
        Long idSolicitud,
        String nombreCategoria,
        MotivoReporte motivo,
        String etiquetaMotivo,
        EstadoReporte estadoReporte,
        AccionModeracion accionTomada,
        String justificacionResolucion,
        OffsetDateTime fechaReporte,
        OffsetDateTime fechaResolucion
) {
}
