package co.edu.udea.alamano.cotizacion_tarificacion.dominio;

import java.math.BigDecimal;

public record ResultadoCotizacion(
        BigDecimal tarifaBase,
        BigDecimal precioMin,
        BigDecimal precioMax,
        BigDecimal factorAplicado,
        BigDecimal precioSinAcotar,
        BigDecimal precioEstimado,
        boolean esUrgente,
        boolean recargoUrgenciaAplicado,
        boolean acotadoAlMaximo,
        boolean acotadoAlMinimo,
        String detalleCalculo
) {
}
