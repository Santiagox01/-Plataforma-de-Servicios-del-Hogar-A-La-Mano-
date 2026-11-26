package co.edu.udea.alamano.comun.notificacion;

import co.edu.udea.alamano.comun.excepcion.ExcepcionNoAutorizado;
import co.edu.udea.alamano.comun.excepcion.ExcepcionRecursoNoEncontrado;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ServicioNotificaciones {

    private final NotificacionRepositorio notificacionRepositorio;

    public ServicioNotificaciones(NotificacionRepositorio notificacionRepositorio) {
        this.notificacionRepositorio = notificacionRepositorio;
    }

    @Transactional
    public Notificacion notificar(Long idUsuario, String titulo, String mensaje, String tipo) {
        return notificacionRepositorio.save(new Notificacion(idUsuario, titulo, mensaje, tipo));
    }

    @Transactional(readOnly = true)
    public List<NotificacionDTO> listarPorUsuario(Long idUsuario) {
        return notificacionRepositorio.findByIdUsuarioOrderByFechaCreacionDesc(idUsuario)
                .stream()
                .map(NotificacionDTO::desdeEntidad)
                .toList();
    }

    @Transactional
    public NotificacionDTO marcarComoLeida(Long idNotificacion, Long idUsuario) {
        Notificacion notificacion = notificacionRepositorio.findById(idNotificacion)
                .orElseThrow(() -> new ExcepcionRecursoNoEncontrado("Notificación no encontrada."));
        if (!notificacion.getIdUsuario().equals(idUsuario)) {
            throw new ExcepcionNoAutorizado("No puedes modificar notificaciones de otro usuario.");
        }
        notificacion.marcarLeida();
        return NotificacionDTO.desdeEntidad(notificacionRepositorio.save(notificacion));
    }
}
