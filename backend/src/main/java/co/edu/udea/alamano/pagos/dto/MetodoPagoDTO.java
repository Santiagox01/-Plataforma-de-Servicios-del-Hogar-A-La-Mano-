package co.edu.udea.alamano.pagos.dto;

import co.edu.udea.alamano.pagos.dominio.FranquiciaTarjeta;
import co.edu.udea.alamano.pagos.dominio.TipoMetodoPago;

import java.time.OffsetDateTime;

public record MetodoPagoDTO(
        Long idMetodoPago,
        String token,
        TipoMetodoPago tipo,
        FranquiciaTarjeta franquicia,
        String ultimosDigitos,
        String titular,
        Integer mesVencimiento,
        Integer anioVencimiento,
        String resumen,
        OffsetDateTime fechaRegistro
) {
}
