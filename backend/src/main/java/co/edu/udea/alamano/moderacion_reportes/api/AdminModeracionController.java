package co.edu.udea.alamano.moderacion_reportes.api;

import co.edu.udea.alamano.autenticacion_seguridad.seguridad.DetallesUsuario;
import co.edu.udea.alamano.moderacion_reportes.aplicacion.ServicioModeracion;
import co.edu.udea.alamano.moderacion_reportes.dominio.EstadoReporte;
import co.edu.udea.alamano.moderacion_reportes.dto.ReporteAdminDTO;
import co.edu.udea.alamano.moderacion_reportes.dto.ResolverReporteDTO;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/reportes")
@PreAuthorize("hasRole('ADMINISTRADOR')")
public class AdminModeracionController {

    private final ServicioModeracion servicioModeracion;

    public AdminModeracionController(ServicioModeracion servicioModeracion) {
        this.servicioModeracion = servicioModeracion;
    }

    @GetMapping
    public ResponseEntity<List<ReporteAdminDTO>> listarCola(@RequestParam(required = false) EstadoReporte estado) {
        return ResponseEntity.ok(servicioModeracion.listarCola(estado));
    }

    @PostMapping("/{idReporte}/resolver")
    public ResponseEntity<ReporteAdminDTO> resolver(@PathVariable Long idReporte,
                                                    @Valid @RequestBody ResolverReporteDTO dto,
                                                    @AuthenticationPrincipal DetallesUsuario admin) {
        return ResponseEntity.ok(servicioModeracion.resolverReporte(idReporte, admin.getIdUsuario(), dto));
    }
}
