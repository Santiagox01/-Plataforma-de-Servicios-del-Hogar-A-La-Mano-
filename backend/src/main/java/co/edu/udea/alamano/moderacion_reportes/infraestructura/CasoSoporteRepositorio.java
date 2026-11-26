package co.edu.udea.alamano.moderacion_reportes.infraestructura;

import co.edu.udea.alamano.moderacion_reportes.dominio.CasoSoporte;
import co.edu.udea.alamano.moderacion_reportes.dominio.EstadoCasoSoporte;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CasoSoporteRepositorio extends JpaRepository<CasoSoporte, Long> {

    @Query("""
            SELECT c FROM CasoSoporte c
            JOIN FETCH c.usuario
            LEFT JOIN FETCH c.solicitud
            WHERE c.idCaso = :idCaso
            """)
    Optional<CasoSoporte> buscarPorIdConDetalles(@Param("idCaso") Long idCaso);

    @Query("""
            SELECT c FROM CasoSoporte c
            JOIN FETCH c.usuario
            LEFT JOIN FETCH c.solicitud
            WHERE c.usuario.idUsuario = :idUsuario
            ORDER BY c.fechaCreacion DESC
            """)
    List<CasoSoporte> listarPorUsuario(@Param("idUsuario") Long idUsuario);

    @Query("""
            SELECT c FROM CasoSoporte c
            JOIN FETCH c.usuario
            LEFT JOIN FETCH c.solicitud
            ORDER BY CASE WHEN c.estadoCaso = co.edu.udea.alamano.moderacion_reportes.dominio.EstadoCasoSoporte.ABIERTO THEN 0 ELSE 1 END,
                     c.fechaCreacion ASC
            """)
    List<CasoSoporte> listarTodosAdmin();

    Optional<CasoSoporte> findFirstBySolicitudIdSolicitudAndEstadoCaso(Long idSolicitud, EstadoCasoSoporte estadoCaso);
}
