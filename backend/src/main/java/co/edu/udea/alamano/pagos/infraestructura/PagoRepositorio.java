package co.edu.udea.alamano.pagos.infraestructura;

import co.edu.udea.alamano.pagos.dominio.EstadoPago;
import co.edu.udea.alamano.pagos.dominio.Pago;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface PagoRepositorio extends JpaRepository<Pago, Long> {

    @Query("""
            SELECT p FROM Pago p
            JOIN FETCH p.solicitud s
            JOIN FETCH s.cliente
            JOIN FETCH s.profesional
            JOIN FETCH s.servicio srv
            JOIN FETCH srv.categoriaOficio
            JOIN FETCH p.metodoPago
            WHERE s.idSolicitud = :idSolicitud
              AND p.estadoPago IN :estados
            """)
    Optional<Pago> buscarPagoActivoPorSolicitud(@Param("idSolicitud") Long idSolicitud,
                                                @Param("estados") Collection<EstadoPago> estados);

    @Query("""
            SELECT p FROM Pago p
            JOIN FETCH p.solicitud s
            JOIN FETCH s.cliente
            JOIN FETCH s.profesional
            JOIN FETCH s.servicio srv
            JOIN FETCH srv.categoriaOficio
            JOIN FETCH p.metodoPago
            WHERE s.idSolicitud = :idSolicitud
              AND p.estadoPago = co.edu.udea.alamano.pagos.dominio.EstadoPago.PENDIENTE_APROBACION
            """)
    Optional<Pago> buscarPendienteAprobacionPorSolicitud(@Param("idSolicitud") Long idSolicitud);

    @Query("""
            SELECT p FROM Pago p
            JOIN FETCH p.solicitud s
            JOIN FETCH s.cliente
            JOIN FETCH s.profesional
            JOIN FETCH s.servicio srv
            JOIN FETCH srv.categoriaOficio
            JOIN FETCH p.metodoPago
            WHERE p.referenciaOrdenExterna = :orden
            """)
    Optional<Pago> buscarPorOrdenExterna(@Param("orden") String referenciaOrdenExterna);

    @Query("""
            SELECT p FROM Pago p
            JOIN FETCH p.solicitud s
            JOIN FETCH s.cliente
            JOIN FETCH s.profesional
            JOIN FETCH s.servicio srv
            JOIN FETCH srv.categoriaOficio
            JOIN FETCH p.metodoPago
            WHERE s.idSolicitud = :idSolicitud
            ORDER BY p.fechaPago DESC, p.idPago DESC
            """)
    List<Pago> listarHistorialPorSolicitud(@Param("idSolicitud") Long idSolicitud);
}
