package co.edu.udea.alamano.pagos.dominio;

public record ResultadoTransaccionPasarela(
        boolean aprobado,
        String referenciaPasarela,
        String motivoRechazo
) {
}
