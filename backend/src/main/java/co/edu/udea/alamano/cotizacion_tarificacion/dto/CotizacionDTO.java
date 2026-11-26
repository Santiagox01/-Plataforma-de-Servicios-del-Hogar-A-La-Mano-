package co.edu.udea.alamano.cotizacion_tarificacion.dto;

import java.math.BigDecimal;

public record CotizacionDTO(
        Long idServicio,
        String nombreCategoria,
        BigDecimal tarifaBase,
        BigDecimal precioMin,
        BigDecimal precioMax,
        BigDecimal factorAplicado,
        BigDecimal precioEstimado,
        BigDecimal tasaGarantiaInformativa,
        boolean esUrgente,
        boolean recargoUrgenciaAplicado,
        boolean acotadoAlMaximo,
        boolean acotadoAlMinimo,
        String detalleCalculo
) {
}
