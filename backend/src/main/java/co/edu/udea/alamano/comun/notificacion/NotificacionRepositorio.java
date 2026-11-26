package co.edu.udea.alamano.comun.notificacion;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificacionRepositorio extends JpaRepository<Notificacion, Long> {
    List<Notificacion> findByIdUsuarioOrderByFechaCreacionDesc(Long idUsuario);
}
