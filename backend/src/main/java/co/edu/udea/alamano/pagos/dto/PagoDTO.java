package co.edu.udea.alamano.pagos.dto;

import co.edu.udea.alamano.pagos.dominio.EstadoPago;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record PagoDTO(
        Long idPago,
        Long idSolicitud,
        EstadoPago estadoPago,
        String pasarela,
        BigDecimal montoServicio,
        BigDecimal tasaGarantia,
        BigDecimal cantidad,
        BigDecimal montoUsd,
        BigDecimal tasaCambioUsd,
        String referenciaIdempotencia,
        String referenciaPasarela,
        String referenciaOrdenExterna,
        String urlAprobacion,
        OffsetDateTime autorizacionExpiraEn,
        String motivoRechazo,
        boolean idempotente,
        OffsetDateTime fechaPago,
        ComprobantePagoDTO comprobante
) {
}
