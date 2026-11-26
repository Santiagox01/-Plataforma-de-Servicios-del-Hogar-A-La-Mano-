package co.edu.udea.alamano.pagos.dominio;

/**
 * Datos transitorios enviados a la pasarela para tokenizacion; nunca expone PAN ni CVV en logs.
 */
public record DatosTarjetaValidacion(
        String numeroTarjeta,
        String cvv,
        int mesVencimiento,
        int anioVencimiento,
        TipoMetodoPago tipo,
        String titular
) {
    @Override
    public String toString() {
        String ultimos = (numeroTarjeta != null && numeroTarjeta.length() >= 4)
                ? numeroTarjeta.substring(numeroTarjeta.length() - 4)
                : "****";
        return "DatosTarjetaValidacion[tipo=" + tipo + ", ultimosDigitos=****" + ultimos + ", titular=" + titular + "]";
    }
}
