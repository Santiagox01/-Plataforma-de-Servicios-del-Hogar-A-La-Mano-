package co.edu.udea.alamano.pagos.dto;

import java.math.BigDecimal;

public record ConfiguracionPagosDTO(
        String pasarelaActiva,
        boolean requiereAprobacionExterna,
        String monedaExterna,
        BigDecimal tasaCambioCopUsd
) {
}
