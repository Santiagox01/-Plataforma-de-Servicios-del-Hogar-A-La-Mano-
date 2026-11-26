package co.edu.udea.alamano.autenticacion_seguridad.api;

import co.edu.udea.alamano.autenticacion_seguridad.aplicacion.ServicioVerificacion;
import co.edu.udea.alamano.autenticacion_seguridad.dto.ProfesionalPendienteDTO;
import co.edu.udea.alamano.autenticacion_seguridad.dto.RechazoVerificacionDTO;
import co.edu.udea.alamano.autenticacion_seguridad.dto.ResultadoVerificacionDTO;
import co.edu.udea.alamano.autenticacion_seguridad.dto.UrlDocumentoDTO;
import co.edu.udea.alamano.autenticacion_seguridad.seguridad.DetallesUsuario;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/profesionales")
@PreAuthorize("hasRole('ADMINISTRADOR')")
public class AdminVerificacionController {

    private final ServicioVerificacion servicioVerificacion;

    public AdminVerificacionController(ServicioVerificacion servicioVerificacion) {
        this.servicioVerificacion = servicioVerificacion;
    }

    @GetMapping("/pendientes")
    public ResponseEntity<List<ProfesionalPendienteDTO>> listarPendientes() {
        return ResponseEntity.ok(servicioVerificacion.listarPendientes());
    }

    @GetMapping("/{id}/documento-url")
    public ResponseEntity<UrlDocumentoDTO> obtenerUrlsDocumentos(@PathVariable Long id) {
        return ResponseEntity.ok(servicioVerificacion.generarUrlsDocumentos(id));
    }

    @PostMapping("/{id}/aprobar")
    public ResponseEntity<ResultadoVerificacionDTO> aprobar(@PathVariable Long id,
                                                            @AuthenticationPrincipal DetallesUsuario admin) {
        return ResponseEntity.ok(servicioVerificacion.aprobarProfesional(id, admin.getIdUsuario()));
    }

    @PostMapping("/{id}/rechazar")
    public ResponseEntity<ResultadoVerificacionDTO> rechazar(@PathVariable Long id,
                                                             @Valid @RequestBody RechazoVerificacionDTO solicitud,
                                                             @AuthenticationPrincipal DetallesUsuario admin) {
        return ResponseEntity.ok(servicioVerificacion.rechazarProfesional(id, admin.getIdUsuario(), solicitud.motivo()));
    }
}
