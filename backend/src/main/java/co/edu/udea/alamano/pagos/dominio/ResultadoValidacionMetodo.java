package co.edu.udea.alamano.pagos.dominio;

public record ResultadoValidacionMetodo(
        boolean valido,
        String token,
        FranquiciaTarjeta franquicia,
        String ultimosDigitos,
        String motivoRechazo
) {
}
