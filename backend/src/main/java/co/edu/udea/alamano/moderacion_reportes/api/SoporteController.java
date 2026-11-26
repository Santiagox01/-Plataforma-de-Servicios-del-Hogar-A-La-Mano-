package co.edu.udea.alamano.moderacion_reportes.api;

import co.edu.udea.alamano.autenticacion_seguridad.seguridad.DetallesUsuario;
import co.edu.udea.alamano.moderacion_reportes.aplicacion.ServicioSoporte;
import co.edu.udea.alamano.moderacion_reportes.dto.CasoSoporteDTO;
import co.edu.udea.alamano.moderacion_reportes.dto.CrearCasoSoporteDTO;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/soporte")
@PreAuthorize("hasAnyRole('CLIENTE', 'PROFESIONAL')")
public class SoporteController {

    private final ServicioSoporte servicioSoporte;

    public SoporteController(ServicioSoporte servicioSoporte) {
        this.servicioSoporte = servicioSoporte;
    }

    @PostMapping("/casos")
    public ResponseEntity<CasoSoporteDTO> crear(@Valid @RequestBody CrearCasoSoporteDTO dto,
                                                @AuthenticationPrincipal DetallesUsuario usuario) {
        CasoSoporteDTO creado = servicioSoporte.crearCaso(usuario.getIdUsuario(), dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(creado);
    }

    @GetMapping("/mis-casos")
    public ResponseEntity<List<CasoSoporteDTO>> misCasos(@AuthenticationPrincipal DetallesUsuario usuario) {
        return ResponseEntity.ok(servicioSoporte.listarMisCasos(usuario.getIdUsuario()));
    }
}
