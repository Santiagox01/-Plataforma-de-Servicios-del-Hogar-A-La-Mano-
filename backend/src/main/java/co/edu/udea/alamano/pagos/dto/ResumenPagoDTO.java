package co.edu.udea.alamano.pagos.dto;

import co.edu.udea.alamano.gestion_servicios.dominio.EstadoSolicitud;

import java.math.BigDecimal;

public record ResumenPagoDTO(
        Long idSolicitud,
        EstadoSolicitud estadoSolicitud,
        String pasarelaActiva,
        boolean requiereAprobacionExterna,
        BigDecimal montoServicio,
        BigDecimal tasaGarantia,
        BigDecimal totalAPagar,
        BigDecimal montoUsd,
        BigDecimal tasaCambioUsd,
        boolean puedePagar,
        boolean puedeLiberarCustodia,
        boolean tieneReporteAbierto,
        PagoDTO pagoActual
) {
}
