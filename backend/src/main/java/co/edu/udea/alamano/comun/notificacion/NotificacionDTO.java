package co.edu.udea.alamano.comun.notificacion;

import java.time.OffsetDateTime;

public record NotificacionDTO(
        Long idNotificacion,
        String titulo,
        String mensaje,
        String tipo,
        boolean leida,
        OffsetDateTime fechaCreacion
) {
    public static NotificacionDTO desdeEntidad(Notificacion n) {
        return new NotificacionDTO(
                n.getIdNotificacion(),
                n.getTitulo(),
                n.getMensaje(),
                n.getTipo(),
                n.isLeida(),
                n.getFechaCreacion()
        );
    }
}
