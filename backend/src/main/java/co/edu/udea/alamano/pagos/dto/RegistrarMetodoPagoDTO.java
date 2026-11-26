package co.edu.udea.alamano.pagos.dto;

import co.edu.udea.alamano.pagos.dominio.TipoMetodoPago;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Datos de entrada para registrar un metodo de pago; nunca expone PAN ni CVV en toString().
 */
public record RegistrarMetodoPagoDTO(
        @NotBlank(message = "El número de tarjeta es obligatorio.")
        String numeroTarjeta,
        @NotBlank(message = "El código CVV es obligatorio.")
        String cvv,
        @NotNull(message = "El mes de vencimiento es obligatorio.")
        @Min(value = 1, message = "El mes de vencimiento debe estar entre 1 y 12.")
        @Max(value = 12, message = "El mes de vencimiento debe estar entre 1 y 12.")
        Integer mesVencimiento,
        @NotNull(message = "El año de vencimiento es obligatorio.")
        @Min(value = 2020, message = "El año de vencimiento no es válido.")
        Integer anioVencimiento,
        @NotNull(message = "El tipo de tarjeta es obligatorio.")
        TipoMetodoPago tipo,
        @NotBlank(message = "El nombre del titular es obligatorio.")
        String titular
) {
    @Override
    public String toString() {
        return "RegistrarMetodoPagoDTO[tipo=" + tipo + ", titular=" + titular + ", tarjeta=****]";
    }
}
