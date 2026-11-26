package co.edu.udea.alamano.comun.excepcion;

import java.time.OffsetDateTime;
import java.util.Map;

public record RespuestaError(
        OffsetDateTime marcaTiempo,
        int estado,
        String error,
        String mensaje,
        String ruta,
        Map<String, String> detalles
) {
}
