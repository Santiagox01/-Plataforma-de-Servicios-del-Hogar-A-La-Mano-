package co.edu.udea.alamano.pagos.dto;

import co.edu.udea.alamano.pagos.dominio.EstadoPago;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record ComprobantePagoDTO(
        String numeroComprobante,
        Long idSolicitud,
        String nombreCategoria,
        String nombreCliente,
        String nombreProfesional,
        String metodoPagoResumen,
        String pasarela,
        BigDecimal montoServicio,
        BigDecimal tasaGarantia,
        BigDecimal totalPagado,
        BigDecimal montoUsd,
        BigDecimal tasaCambioUsd,
        EstadoPago estadoPago,
        String referenciaPasarela,
        OffsetDateTime fechaPago
) {
}
