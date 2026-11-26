package co.edu.udea.alamano.cotizacion_tarificacion.api;

import co.edu.udea.alamano.autenticacion_seguridad.seguridad.DetallesUsuario;
import co.edu.udea.alamano.cotizacion_tarificacion.aplicacion.ServicioTarifasProfesional;
import co.edu.udea.alamano.cotizacion_tarificacion.dto.ConfigurarTarifaDTO;
import co.edu.udea.alamano.cotizacion_tarificacion.dto.ServicioTarifaDTO;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/profesionales/me")
@PreAuthorize("hasRole('PROFESIONAL')")
public class TarifaProfesionalController {

    private final ServicioTarifasProfesional servicioTarifas;

    public TarifaProfesionalController(ServicioTarifasProfesional servicioTarifas) {
        this.servicioTarifas = servicioTarifas;
    }

    @GetMapping("/servicios")
    public ResponseEntity<List<ServicioTarifaDTO>> listarMisServicios(@AuthenticationPrincipal DetallesUsuario profesional) {
        return ResponseEntity.ok(servicioTarifas.listarMisServicios(profesional.getIdUsuario()));
    }

    @PutMapping("/tarifas")
    public ResponseEntity<ServicioTarifaDTO> configurarTarifa(@Valid @RequestBody ConfigurarTarifaDTO dto,
                                                              @AuthenticationPrincipal DetallesUsuario profesional) {
        return ResponseEntity.ok(servicioTarifas.configurarTarifa(profesional.getIdUsuario(), dto));
    }
}
