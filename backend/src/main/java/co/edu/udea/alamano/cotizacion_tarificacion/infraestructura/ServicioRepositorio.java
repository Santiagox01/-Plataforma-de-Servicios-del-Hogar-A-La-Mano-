package co.edu.udea.alamano.cotizacion_tarificacion.infraestructura;

import co.edu.udea.alamano.cotizacion_tarificacion.dominio.Servicio;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ServicioRepositorio extends JpaRepository<Servicio, Long> {
    List<Servicio> findByProfesionalIdUsuario(Long idProfesional);

    Optional<Servicio> findByProfesionalIdUsuarioAndCategoriaOficioIdCategoria(Long idProfesional, Long idCategoria);
}
