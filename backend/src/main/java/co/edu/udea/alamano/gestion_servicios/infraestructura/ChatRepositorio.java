package co.edu.udea.alamano.gestion_servicios.infraestructura;

import co.edu.udea.alamano.gestion_servicios.dominio.Chat;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ChatRepositorio extends JpaRepository<Chat, Long> {
    Optional<Chat> findBySolicitudIdSolicitud(Long idSolicitud);
}
