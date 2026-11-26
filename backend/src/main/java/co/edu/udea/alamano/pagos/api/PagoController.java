package co.edu.udea.alamano.pagos.api;

import co.edu.udea.alamano.autenticacion_seguridad.seguridad.DetallesUsuario;
import co.edu.udea.alamano.pagos.aplicacion.ServicioPagos;
import co.edu.udea.alamano.pagos.dominio.EstadoPago;
import co.edu.udea.alamano.pagos.dto.ConfirmarPagoExternoDTO;
import co.edu.udea.alamano.pagos.dto.IniciarPagoDTO;
import co.edu.udea.alamano.pagos.dto.PagoDTO;
import co.edu.udea.alamano.pagos.dto.ProcesarPagoDTO;
import co.edu.udea.alamano.pagos.dto.RespuestaInicioPagoDTO;
import co.edu.udea.alamano.pagos.dto.ResumenPagoDTO;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/solicitudes/{idSolicitud}")
public class PagoController {

    private final ServicioPagos servicioPagos;

    public PagoController(ServicioPagos servicioPagos) {
        this.servicioPagos = servicioPagos;
    }

    @GetMapping("/pago/resumen")
    @PreAuthorize("hasAnyRole('CLIENTE', 'PROFESIONAL')")
    public ResponseEntity<ResumenPagoDTO> obtenerResumen(@PathVariable Long idSolicitud,
                                                         @AuthenticationPrincipal DetallesUsuario usuario) {
        return ResponseEntity.ok(servicioPagos.obtenerResumenPago(idSolicitud, usuario.getIdUsuario()));
    }

    @GetMapping("/pago")
    @PreAuthorize("hasAnyRole('CLIENTE', 'PROFESIONAL')")
    public ResponseEntity<PagoDTO> obtenerPago(@PathVariable Long idSolicitud,
                                               @AuthenticationPrincipal DetallesUsuario usuario) {
        return ResponseEntity.ok(servicioPagos.obtenerPagoDeSolicitud(idSolicitud, usuario.getIdUsuario()));
    }

    @PostMapping("/pagar")
    @PreAuthorize("hasRole('CLIENTE')")
    public ResponseEntity<PagoDTO> pagar(@PathVariable Long idSolicitud,
                                         @Valid @RequestBody ProcesarPagoDTO dto,
                                         @AuthenticationPrincipal DetallesUsuario cliente) {
        PagoDTO resultado = servicioPagos.procesarPago(idSolicitud, cliente.getIdUsuario(), dto.idMetodoPago());
        HttpStatus estadoHttp = (!resultado.idempotente() && resultado.estadoPago() == EstadoPago.RETENIDO)
                ? HttpStatus.CREATED
                : HttpStatus.OK;
        return ResponseEntity.status(estadoHttp).body(resultado);
    }

    @PostMapping("/pagar/iniciar")
    @PreAuthorize("hasRole('CLIENTE')")
    public ResponseEntity<RespuestaInicioPagoDTO> iniciarPagoExterno(
            @PathVariable Long idSolicitud,
            @RequestBody(required = false) IniciarPagoDTO dto,
            @AuthenticationPrincipal DetallesUsuario cliente) {
        String appRedirect = dto != null ? dto.appRedirectUrl() : null;
        RespuestaInicioPagoDTO resp = servicioPagos.iniciarPagoExterno(idSolicitud, cliente.getIdUsuario(), appRedirect);
        HttpStatus estadoHttp = resp.idempotente() ? HttpStatus.OK : HttpStatus.CREATED;
        return ResponseEntity.status(estadoHttp).body(resp);
    }

    @PostMapping("/pagar/confirmar")
    @PreAuthorize("hasRole('CLIENTE')")
    public ResponseEntity<PagoDTO> confirmarPagoExterno(
            @PathVariable Long idSolicitud,
            @RequestBody(required = false) ConfirmarPagoExternoDTO dto,
            @AuthenticationPrincipal DetallesUsuario cliente) {
        String idOrden = dto != null ? dto.idOrdenExterna() : null;
        return ResponseEntity.ok(servicioPagos.confirmarPagoExterno(idSolicitud, cliente.getIdUsuario(), idOrden));
    }

    @PostMapping("/pagar/cancelar")
    @PreAuthorize("hasRole('CLIENTE')")
    public ResponseEntity<PagoDTO> cancelarPagoPendiente(@PathVariable Long idSolicitud,
                                                         @AuthenticationPrincipal DetallesUsuario cliente) {
        return ResponseEntity.ok(servicioPagos.cancelarPagoPendiente(idSolicitud, cliente.getIdUsuario()));
    }

    @PostMapping("/pago/liberar")
    @PreAuthorize("hasRole('CLIENTE')")
    public ResponseEntity<PagoDTO> liberarCustodia(@PathVariable Long idSolicitud,
                                                   @AuthenticationPrincipal DetallesUsuario cliente) {
        return ResponseEntity.ok(servicioPagos.liberarCustodiaPorCliente(idSolicitud, cliente.getIdUsuario()));
    }
}
