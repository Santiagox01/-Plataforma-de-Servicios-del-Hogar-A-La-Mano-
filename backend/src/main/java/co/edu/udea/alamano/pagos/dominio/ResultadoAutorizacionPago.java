package co.edu.udea.alamano.pagos.dominio;

import java.time.OffsetDateTime;

public record ResultadoAutorizacionPago(
        boolean aprobado,
        String idOrdenExterna,
        String idAutorizacion,
        OffsetDateTime expiraEn,
        String motivoRechazo
) {
}
