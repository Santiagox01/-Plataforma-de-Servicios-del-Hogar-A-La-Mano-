package co.edu.udea.alamano.moderacion_reportes.aplicacion;

import co.edu.udea.alamano.gestion_servicios.aplicacion.IConsultaReportesAbiertos;
import co.edu.udea.alamano.moderacion_reportes.dominio.EstadoReporte;
import co.edu.udea.alamano.moderacion_reportes.infraestructura.ReporteRepositorio;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class ConsultaReportesAbiertosAdaptador implements IConsultaReportesAbiertos {

    private final ReporteRepositorio reporteRepositorio;

    public ConsultaReportesAbiertosAdaptador(ReporteRepositorio reporteRepositorio) {
        this.reporteRepositorio = reporteRepositorio;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existeReporteAbierto(Long idSolicitud) {
        return reporteRepositorio.existsBySolicitudIdSolicitudAndEstadoReporte(idSolicitud, EstadoReporte.PENDIENTE);
    }
}
