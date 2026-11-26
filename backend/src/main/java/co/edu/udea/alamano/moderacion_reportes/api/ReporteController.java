package co.edu.udea.alamano.moderacion_reportes.api;

import co.edu.udea.alamano.autenticacion_seguridad.seguridad.DetallesUsuario;
import co.edu.udea.alamano.moderacion_reportes.aplicacion.ServicioReportes;
import co.edu.udea.alamano.moderacion_reportes.dto.CrearReporteDTO;
import co.edu.udea.alamano.moderacion_reportes.dto.MotivoReporteDTO;
import co.edu.udea.alamano.moderacion_reportes.dto.ReporteVistaReportadoDTO;
import co.edu.udea.alamano.moderacion_reportes.dto.ReporteVistaReportanteDTO;
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
@RequestMapping("/api/reportes")
@PreAuthorize("hasAnyRole('CLIENTE', 'PROFESIONAL')")
public class ReporteController {

    private final ServicioReportes servicioReportes;

    public ReporteController(ServicioReportes servicioReportes) {
        this.servicioReportes = servicioReportes;
    }

    @GetMapping("/motivos")
    public ResponseEntity<List<MotivoReporteDTO>> listarMotivos(@AuthenticationPrincipal DetallesUsuario usuario) {
        return ResponseEntity.ok(servicioReportes.listarMotivosPorRol(usuario.getRol()));
    }

    @PostMapping
    public ResponseEntity<ReporteVistaReportanteDTO> crear(@RequestBody CrearReporteDTO dto,
                                                           @AuthenticationPrincipal DetallesUsuario usuario) {
        ReporteVistaReportanteDTO creado = servicioReportes.crearReporte(
                usuario.getIdUsuario(), usuario.getRol(), dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(creado);
    }

    @GetMapping("/mis-enviados")
    public ResponseEntity<List<ReporteVistaReportanteDTO>> misEnviados(@AuthenticationPrincipal DetallesUsuario usuario) {
        return ResponseEntity.ok(servicioReportes.listarEnviados(usuario.getIdUsuario()));
    }

    @GetMapping("/recibidos")
    public ResponseEntity<List<ReporteVistaReportadoDTO>> recibidos(@AuthenticationPrincipal DetallesUsuario usuario) {
        return ResponseEntity.ok(servicioReportes.listarRecibidos(usuario.getIdUsuario()));
    }
}
