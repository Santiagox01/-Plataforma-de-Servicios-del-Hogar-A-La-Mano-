package co.edu.udea.alamano.gestion_servicios.infraestructura;

import co.edu.udea.alamano.gestion_servicios.dominio.Solicitud;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface JpaSolicitudRepositorio extends JpaRepository<Solicitud, Long> {

    @Query("""
            SELECT DISTINCT s FROM Solicitud s
            JOIN FETCH s.cliente
            JOIN FETCH s.profesional
            JOIN FETCH s.servicio srv
            JOIN FETCH srv.categoriaOficio
            LEFT JOIN FETCH s.resena
            LEFT JOIN FETCH s.chat
            WHERE s.idSolicitud = :id
            """)
    Optional<Solicitud> buscarDetallePorId(@Param("id") Long id);

    @Query("""
            SELECT DISTINCT s FROM Solicitud s
            JOIN FETCH s.cliente
            JOIN FETCH s.profesional
            JOIN FETCH s.servicio srv
            JOIN FETCH srv.categoriaOficio
            LEFT JOIN FETCH s.resena
            WHERE s.cliente.idUsuario = :idCliente
            ORDER BY s.fechaSolicitud DESC
            """)
    List<Solicitud> listarPorCliente(@Param("idCliente") Long idCliente);

    @Query("""
            SELECT DISTINCT s FROM Solicitud s
            JOIN FETCH s.cliente
            JOIN FETCH s.profesional
            JOIN FETCH s.servicio srv
            JOIN FETCH srv.categoriaOficio
            LEFT JOIN FETCH s.resena
            WHERE s.profesional.idUsuario = :idProfesional
            ORDER BY s.fechaSolicitud DESC
            """)
    List<Solicitud> listarPorProfesional(@Param("idProfesional") Long idProfesional);
}
