package co.edu.udea.alamano.cotizacion_tarificacion.dto;

import java.math.BigDecimal;

public record ServicioTarifaDTO(
        Long idServicio,
        Long idCategoria,
        String nombreCategoria,
        BigDecimal tarifaBase,
        BigDecimal precioMin,
        BigDecimal precioMax,
        boolean tarifaConfigurada
) {
}
