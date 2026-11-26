package co.edu.udea.alamano.gestion_servicios.aplicacion;

import java.math.BigDecimal;
import java.util.Optional;

/**
 * Puerto de lectura para consultar el estado actual del pago asociado a una solicitud.
 */
public interface IConsultaPagoSolicitud {

    Optional<ResumenPagoSolicitud> consultarPagoActual(Long idSolicitud);

    record ResumenPagoSolicitud(
            Long idPago,
            String estadoPago,
            BigDecimal montoServicio,
            BigDecimal tasaGarantia,
            BigDecimal totalPagado,
            String numeroComprobante,
            boolean estaRetenido,
            boolean estaRechazado
    ) {
    }
}
