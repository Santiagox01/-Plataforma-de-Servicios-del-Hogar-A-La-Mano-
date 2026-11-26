package co.edu.udea.alamano.gestion_servicios.aplicacion;

/**
 * Puerto de consulta para verificar si una solicitud tiene reportes activos pendientes de resolucion.
 */
public interface IConsultaReportesAbiertos {

    boolean existeReporteAbierto(Long idSolicitud);
}
