package co.edu.udea.alamano.pagos.dominio;

public record ResultadoInicioPago(
        boolean creado,
        String idOrdenExterna,
        String urlAprobacion,
        String motivoError
) {
}
