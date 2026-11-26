package co.edu.udea.alamano.moderacion_reportes.api;

import co.edu.udea.alamano.autenticacion_seguridad.seguridad.DetallesUsuario;
import co.edu.udea.alamano.moderacion_reportes.aplicacion.ServicioSoporte;
import co.edu.udea.alamano.moderacion_reportes.dto.CasoSoporteDTO;
import co.edu.udea.alamano.moderacion_reportes.dto.ResponderCasoSoporteDTO;
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
@RequestMapping("/api/admin/soporte")
@PreAuthorize("hasRole('ADMINISTRADOR')")
public class AdminSoporteController {

    private final ServicioSoporte servicioSoporte;

    public AdminSoporteController(ServicioSoporte servicioSoporte) {
        this.servicioSoporte = servicioSoporte;
    }

    @GetMapping("/casos")
    public ResponseEntity<List<CasoSoporteDTO>> listarTodos() {
        return ResponseEntity.ok(servicioSoporte.listarTodosAdmin());
    }

    @PostMapping("/casos/{idCaso}/responder")
    public ResponseEntity<CasoSoporteDTO> responder(@PathVariable Long idCaso,
                                                    @Valid @RequestBody ResponderCasoSoporteDTO dto,
                                                    @AuthenticationPrincipal DetallesUsuario admin) {
        return ResponseEntity.ok(servicioSoporte.responderCaso(idCaso, admin.getIdUsuario(), dto));
    }
}
