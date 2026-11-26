package co.edu.udea.alamano.moderacion_reportes.infraestructura;

import co.edu.udea.alamano.moderacion_reportes.dominio.EstadoReporte;
import co.edu.udea.alamano.moderacion_reportes.dominio.Reporte;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ReporteRepositorio extends JpaRepository<Reporte, Long> {

    long countByReportadoIdUsuario(Long idReportado);

    long countByReportanteIdUsuario(Long idReportante);

    boolean existsBySolicitudIdSolicitudAndEstadoReporte(Long idSolicitud, EstadoReporte estadoReporte);

    @Query("""
            SELECT r FROM Reporte r
            JOIN FETCH r.solicitud s
            JOIN FETCH s.servicio srv
            JOIN FETCH srv.categoriaOficio
            JOIN FETCH r.reportante
            JOIN FETCH r.reportado
            WHERE r.idReporte = :idReporte
            """)
    Optional<Reporte> buscarPorIdConDetalles(@Param("idReporte") Long idReporte);

    @Query("""
            SELECT r FROM Reporte r
            JOIN FETCH r.solicitud s
            JOIN FETCH s.servicio srv
            JOIN FETCH srv.categoriaOficio
            JOIN FETCH r.reportante
            JOIN FETCH r.reportado
            WHERE r.estadoReporte = :estado
            ORDER BY r.esPrioritario DESC, r.fechaReporte ASC
            """)
    List<Reporte> listarPorEstadoOrdenCola(@Param("estado") EstadoReporte estado);

    @Query("""
            SELECT r FROM Reporte r
            JOIN FETCH r.solicitud s
            JOIN FETCH s.servicio srv
            JOIN FETCH srv.categoriaOficio
            JOIN FETCH r.reportante
            JOIN FETCH r.reportado
            ORDER BY CASE WHEN r.estadoReporte = co.edu.udea.alamano.moderacion_reportes.dominio.EstadoReporte.PENDIENTE THEN 0 ELSE 1 END,
                     r.esPrioritario DESC,
                     r.fechaReporte ASC
            """)
    List<Reporte> listarTodosOrdenCola();

    @Query("""
            SELECT r FROM Reporte r
            JOIN FETCH r.solicitud s
            JOIN FETCH s.servicio srv
            JOIN FETCH srv.categoriaOficio
            JOIN FETCH r.reportante
            JOIN FETCH r.reportado
            WHERE r.reportante.idUsuario = :idReportante
            ORDER BY r.fechaReporte DESC
            """)
    List<Reporte> listarPorReportante(@Param("idReportante") Long idReportante);

    @Query("""
            SELECT r FROM Reporte r
            JOIN FETCH r.solicitud s
            JOIN FETCH s.servicio srv
            JOIN FETCH srv.categoriaOficio
            JOIN FETCH r.reportante
            JOIN FETCH r.reportado
            WHERE r.reportado.idUsuario = :idReportado
            ORDER BY r.fechaReporte DESC
            """)
    List<Reporte> listarPorReportado(@Param("idReportado") Long idReportado);
}
