package co.edu.udea.alamano.autenticacion_seguridad.infraestructura;

import co.edu.udea.alamano.autenticacion_seguridad.dominio.Administrador;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AdministradorRepositorio extends JpaRepository<Administrador, Long> {
}
