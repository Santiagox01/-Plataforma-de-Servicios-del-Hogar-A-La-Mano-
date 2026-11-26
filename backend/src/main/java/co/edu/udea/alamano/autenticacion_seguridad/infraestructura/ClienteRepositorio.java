package co.edu.udea.alamano.autenticacion_seguridad.infraestructura;

import co.edu.udea.alamano.autenticacion_seguridad.dominio.Cliente;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClienteRepositorio extends JpaRepository<Cliente, Long> {
}
