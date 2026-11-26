package co.edu.udea.alamano.cotizacion_tarificacion.infraestructura;

import co.edu.udea.alamano.cotizacion_tarificacion.dominio.CategoriaOficio;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CategoriaOficioRepositorio extends JpaRepository<CategoriaOficio, Long> {
    List<CategoriaOficio> findAllByOrderByNombreAsc();
}
