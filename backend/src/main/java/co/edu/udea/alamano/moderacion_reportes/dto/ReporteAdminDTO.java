package co.edu.udea.alamano.moderacion_reportes.dto;

import co.edu.udea.alamano.autenticacion_seguridad.dominio.RolUsuario;
import co.edu.udea.alamano.moderacion_reportes.dominio.AccionModeracion;
import co.edu.udea.alamano.moderacion_reportes.dominio.DecisionPagoModeracion;
import co.edu.udea.alamano.moderacion_reportes.dominio.EstadoReporte;
import co.edu.udea.alamano.moderacion_reportes.dominio.MotivoReporte;

import java.time.OffsetDateTime;

public record ReporteAdminDTO(
        Long idReporte,
        Long idSolicitud,
        String nombreCategoria,
        Long idReportante,
        String nombreReportante,
        RolUsuario rolReportante,
        long reportesPreviosReportante,
        Long idReportado,
        String nombreReportado,
        RolUsuario rolReportado,
        long reportesPreviosReportado,
        MotivoReporte motivo,
        String etiquetaMotivo,
        String descripcion,
        boolean esPrioritario,
        EstadoReporte estadoReporte,
        AccionModeracion accionTomada,
        String justificacionResolucion,
        Integer diasSuspension,
        DecisionPagoModeracion decisionPago,
        String estadoPagoSolicitud,
        boolean tienePagoRetenido,
        Long idAdminResolutor,
        OffsetDateTime fechaReporte,
        OffsetDateTime fechaResolucion
) {
}
