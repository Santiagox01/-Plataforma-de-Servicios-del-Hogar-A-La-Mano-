package co.edu.udea.alamano.gestion_servicios.api;

import co.edu.udea.alamano.autenticacion_seguridad.seguridad.DetallesUsuario;
import co.edu.udea.alamano.gestion_servicios.aplicacion.IMensajeriaChat;
import co.edu.udea.alamano.gestion_servicios.dto.EnviarMensajeDTO;
import co.edu.udea.alamano.gestion_servicios.dto.MensajeDTO;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/solicitudes/{idSolicitud}/chat/mensajes")
@PreAuthorize("hasAnyRole('CLIENTE', 'PROFESIONAL')")
public class ChatController {

    private final IMensajeriaChat mensajeriaChat;

    public ChatController(IMensajeriaChat mensajeriaChat) {
        this.mensajeriaChat = mensajeriaChat;
    }

    @GetMapping
    public ResponseEntity<List<MensajeDTO>> listarMensajes(@PathVariable Long idSolicitud,
                                                           @AuthenticationPrincipal DetallesUsuario usuario) {
        return ResponseEntity.ok(mensajeriaChat.listarMensajes(idSolicitud, usuario.getIdUsuario()));
    }

    @PostMapping
    public ResponseEntity<MensajeDTO> enviarMensaje(@PathVariable Long idSolicitud,
                                                    @Valid @RequestBody EnviarMensajeDTO dto,
                                                    @AuthenticationPrincipal DetallesUsuario usuario) {
        MensajeDTO enviado = mensajeriaChat.enviarMensaje(idSolicitud, usuario.getIdUsuario(), dto.contenido());
        return ResponseEntity.status(HttpStatus.CREATED).body(enviado);
    }

    @PatchMapping("/{idMensaje}/leer")
    public ResponseEntity<MensajeDTO> marcarLeido(@PathVariable Long idSolicitud,
                                                  @PathVariable Long idMensaje,
                                                  @AuthenticationPrincipal DetallesUsuario usuario) {
        return ResponseEntity.ok(mensajeriaChat.marcarComoLeido(idSolicitud, idMensaje, usuario.getIdUsuario()));
    }
}
