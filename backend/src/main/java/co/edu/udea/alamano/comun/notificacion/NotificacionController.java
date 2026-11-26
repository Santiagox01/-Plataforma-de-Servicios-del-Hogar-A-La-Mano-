package co.edu.udea.alamano.comun.notificacion;

import co.edu.udea.alamano.autenticacion_seguridad.seguridad.DetallesUsuario;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/notificaciones")
public class NotificacionController {

    private final ServicioNotificaciones servicioNotificaciones;

    public NotificacionController(ServicioNotificaciones servicioNotificaciones) {
        this.servicioNotificaciones = servicioNotificaciones;
    }

    @GetMapping
    public ResponseEntity<List<NotificacionDTO>> listar(@AuthenticationPrincipal DetallesUsuario usuario) {
        return ResponseEntity.ok(servicioNotificaciones.listarPorUsuario(usuario.getIdUsuario()));
    }

    @PatchMapping("/{id}/leer")
    public ResponseEntity<NotificacionDTO> marcarLeida(@PathVariable Long id,
                                                       @AuthenticationPrincipal DetallesUsuario usuario) {
        return ResponseEntity.ok(servicioNotificaciones.marcarComoLeida(id, usuario.getIdUsuario()));
    }
}
