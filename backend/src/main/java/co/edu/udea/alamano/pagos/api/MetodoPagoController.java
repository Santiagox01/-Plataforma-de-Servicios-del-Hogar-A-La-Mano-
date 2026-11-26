package co.edu.udea.alamano.pagos.api;

import co.edu.udea.alamano.autenticacion_seguridad.seguridad.DetallesUsuario;
import co.edu.udea.alamano.pagos.aplicacion.ServicioMetodosPago;
import co.edu.udea.alamano.pagos.dto.ConfiguracionPagosDTO;
import co.edu.udea.alamano.pagos.dto.MetodoPagoDTO;
import co.edu.udea.alamano.pagos.dto.RegistrarMetodoPagoDTO;
import co.edu.udea.alamano.pagos.dto.RegistrarPayPalDTO;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/metodos-pago")
@PreAuthorize("hasRole('CLIENTE')")
public class MetodoPagoController {

    private final ServicioMetodosPago servicioMetodosPago;

    public MetodoPagoController(ServicioMetodosPago servicioMetodosPago) {
        this.servicioMetodosPago = servicioMetodosPago;
    }

    @GetMapping("/config")
    @PreAuthorize("hasAnyRole('CLIENTE', 'PROFESIONAL')")
    public ResponseEntity<ConfiguracionPagosDTO> obtenerConfiguracion() {
        return ResponseEntity.ok(servicioMetodosPago.obtenerConfiguracionPagos());
    }

    @GetMapping
    public ResponseEntity<List<MetodoPagoDTO>> listar(@AuthenticationPrincipal DetallesUsuario cliente) {
        return ResponseEntity.ok(servicioMetodosPago.listarPorCliente(cliente.getIdUsuario()));
    }

    @PostMapping
    public ResponseEntity<MetodoPagoDTO> registrar(@Valid @RequestBody RegistrarMetodoPagoDTO dto,
                                                   @AuthenticationPrincipal DetallesUsuario cliente) {
        MetodoPagoDTO creado = servicioMetodosPago.registrar(cliente.getIdUsuario(), dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(creado);
    }

    @PostMapping("/paypal")
    public ResponseEntity<MetodoPagoDTO> vincularPayPal(@RequestBody(required = false) RegistrarPayPalDTO dto,
                                                        @AuthenticationPrincipal DetallesUsuario cliente) {
        String alias = dto != null ? dto.correoOAlias() : null;
        MetodoPagoDTO vinculado = servicioMetodosPago.registrarPayPal(cliente.getIdUsuario(), alias);
        return ResponseEntity.status(HttpStatus.CREATED).body(vinculado);
    }

    @DeleteMapping("/{idMetodoPago}")
    public ResponseEntity<Void> eliminar(@PathVariable Long idMetodoPago,
                                         @AuthenticationPrincipal DetallesUsuario cliente) {
        servicioMetodosPago.eliminar(cliente.getIdUsuario(), idMetodoPago);
        return ResponseEntity.noContent().build();
    }
}
