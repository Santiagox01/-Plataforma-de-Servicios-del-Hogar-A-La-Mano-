package co.edu.udea.alamano.pagos.dto;

import jakarta.validation.constraints.NotNull;

public record ProcesarPagoDTO(
        @NotNull(message = "Debes seleccionar un método de pago registrado.")
        Long idMetodoPago
) {
}
