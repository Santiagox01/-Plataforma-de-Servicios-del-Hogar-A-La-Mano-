package co.edu.udea.alamano.gestion_servicios.infraestructura;

import co.edu.udea.alamano.gestion_servicios.dominio.Resena;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ResenaRepositorio extends JpaRepository<Resena, Long> {

    boolean existsBySolicitudIdSolicitud(Long idSolicitud);

    @Query("""
            SELECT r FROM Resena r
            JOIN FETCH r.cliente
            WHERE r.profesional.idUsuario = :idProfesional
            ORDER BY r.fechaResena DESC, r.idResena DESC
            """)
    List<Resena> listarRecientesPorProfesional(@Param("idProfesional") Long idProfesional, Pageable pageable);

    @Query("SELECT COALESCE(SUM(r.estrellas), 0) FROM Resena r WHERE r.profesional.idUsuario = :idProfesional")
    long sumarEstrellasPorProfesional(@Param("idProfesional") Long idProfesional);

    long countByProfesionalIdUsuario(Long idProfesional);
}
