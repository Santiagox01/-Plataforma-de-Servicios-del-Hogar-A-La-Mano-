package co.edu.udea.alamano.gestion_servicios.api;

import co.edu.udea.alamano.autenticacion_seguridad.seguridad.DetallesUsuario;
import co.edu.udea.alamano.gestion_servicios.aplicacion.GestorSolicitudes;
import co.edu.udea.alamano.gestion_servicios.dto.RechazarPrecioFinalDTO;
import co.edu.udea.alamano.gestion_servicios.dto.RechazarSolicitudDTO;
import co.edu.udea.alamano.gestion_servicios.dto.RegistrarPrecioFinalDTO;
import co.edu.udea.alamano.gestion_servicios.dto.RespuestaRechazoSolicitudDTO;
import co.edu.udea.alamano.gestion_servicios.dto.SolicitudDTO;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/solicitudes")
public class SolicitudController {

    private final GestorSolicitudes gestorSolicitudes;

    public SolicitudController(GestorSolicitudes gestorSolicitudes) {
        this.gestorSolicitudes = gestorSolicitudes;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('CLIENTE')")
    public ResponseEntity<SolicitudDTO> crearSolicitud(
            @AuthenticationPrincipal DetallesUsuario cliente,
            @RequestParam Long idServicio,
            @RequestParam String descripcion,
            @RequestParam(defaultValue = "false") boolean esUrgente,
            @RequestPart(value = "fotos", required = false) List<MultipartFile> fotos) {
        SolicitudDTO creada = gestorSolicitudes.crearSolicitud(
                cliente.getIdUsuario(), idServicio, descripcion, esUrgente, fotos);
        return ResponseEntity.status(HttpStatus.CREATED).body(creada);
    }

    @GetMapping("/mis-solicitudes")
    @PreAuthorize("hasAnyRole('CLIENTE', 'PROFESIONAL')")
    public ResponseEntity<List<SolicitudDTO>> listarMisSolicitudes(@AuthenticationPrincipal DetallesUsuario usuario) {
        return ResponseEntity.ok(gestorSolicitudes.listarMisSolicitudes(usuario.getIdUsuario(), usuario.getRol()));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('CLIENTE', 'PROFESIONAL')")
    public ResponseEntity<SolicitudDTO> obtenerDetalle(@PathVariable Long id,
                                                       @AuthenticationPrincipal DetallesUsuario usuario) {
        return ResponseEntity.ok(gestorSolicitudes.obtenerDetalle(id, usuario.getIdUsuario()));
    }

    @PostMapping("/{id}/aceptar")
    @PreAuthorize("hasRole('PROFESIONAL')")
    public ResponseEntity<SolicitudDTO> aceptar(@PathVariable Long id,
                                                @AuthenticationPrincipal DetallesUsuario profesional) {
        return ResponseEntity.ok(gestorSolicitudes.aceptarSolicitud(id, profesional.getIdUsuario()));
    }

    @PostMapping("/{id}/rechazar")
    @PreAuthorize("hasRole('PROFESIONAL')")
    public ResponseEntity<RespuestaRechazoSolicitudDTO> rechazar(
            @PathVariable Long id,
            @Valid @RequestBody(required = false) RechazarSolicitudDTO dto,
            @AuthenticationPrincipal DetallesUsuario profesional) {
        String motivo = dto != null ? dto.motivo() : null;
        return ResponseEntity.ok(gestorSolicitudes.rechazarSolicitud(id, profesional.getIdUsuario(), motivo));
    }

    @PostMapping("/{id}/precio-final")
    @PreAuthorize("hasRole('PROFESIONAL')")
    public ResponseEntity<SolicitudDTO> registrarPrecioFinal(
            @PathVariable Long id,
            @Valid @RequestBody RegistrarPrecioFinalDTO dto,
            @AuthenticationPrincipal DetallesUsuario profesional) {
        return ResponseEntity.ok(gestorSolicitudes.registrarPrecioFinal(id, profesional.getIdUsuario(), dto.precioFinal()));
    }

    @PostMapping("/{id}/confirmar-precio-final")
    @PreAuthorize("hasRole('CLIENTE')")
    public ResponseEntity<SolicitudDTO> confirmarPrecioFinal(@PathVariable Long id,
                                                             @AuthenticationPrincipal DetallesUsuario cliente) {
        return ResponseEntity.ok(gestorSolicitudes.confirmarPrecioFinal(id, cliente.getIdUsuario()));
    }

    @PostMapping("/{id}/rechazar-precio-final")
    @PreAuthorize("hasRole('CLIENTE')")
    public ResponseEntity<SolicitudDTO> rechazarPrecioFinal(
            @PathVariable Long id,
            @Valid @RequestBody(required = false) RechazarPrecioFinalDTO dto,
            @AuthenticationPrincipal DetallesUsuario cliente) {
        String motivo = dto != null ? dto.motivo() : null;
        return ResponseEntity.ok(gestorSolicitudes.rechazarPrecioFinal(id, cliente.getIdUsuario(), motivo));
    }
}
