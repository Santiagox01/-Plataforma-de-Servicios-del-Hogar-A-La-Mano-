package co.edu.udea.alamano.pagos.dto;

import co.edu.udea.alamano.pagos.dominio.EstadoPago;

import java.math.BigDecimal;

public record RespuestaInicioPagoDTO(
        Long idPago,
        Long idSolicitud,
        EstadoPago estadoPago,
        String idOrdenExterna,
        String urlAprobacion,
        BigDecimal montoServicioCop,
        BigDecimal tasaGarantiaCop,
        BigDecimal totalCop,
        BigDecimal montoUsd,
        BigDecimal tasaCambioUsd,
        boolean idempotente
) {
}
