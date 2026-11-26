package co.edu.udea.alamano.autenticacion_seguridad.infraestructura;

import co.edu.udea.alamano.autenticacion_seguridad.dominio.EstadoVerificacion;
import co.edu.udea.alamano.autenticacion_seguridad.dominio.Profesional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ProfesionalRepositorio extends JpaRepository<Profesional, Long> {

    List<Profesional> findByEstadoVerificacionOrderByFechaRegistroAsc(EstadoVerificacion estadoVerificacion);

    @Query("""
            SELECT DISTINCT p FROM Profesional p
            LEFT JOIN FETCH p.servicios s
            LEFT JOIN FETCH s.categoriaOficio
            WHERE p.idUsuario = :id
            """)
    Optional<Profesional> buscarPorIdConServicios(@Param("id") Long id);

    @Query("""
            SELECT DISTINCT p FROM Profesional p
            JOIN FETCH p.servicios s
            JOIN FETCH s.categoriaOficio c
            WHERE p.estadoCuenta = co.edu.udea.alamano.autenticacion_seguridad.dominio.EstadoCuenta.ACTIVA
              AND p.estadoVerificacion = co.edu.udea.alamano.autenticacion_seguridad.dominio.EstadoVerificacion.VERIFICADO
              AND p.disponible = true
              AND c.idCategoria = :categoriaId
            """)
    List<Profesional> buscarElegiblesPorCategoria(@Param("categoriaId") Long categoriaId);
}
