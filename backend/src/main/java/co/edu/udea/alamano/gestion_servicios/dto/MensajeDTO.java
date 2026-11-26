package co.edu.udea.alamano.gestion_servicios.dto;

import co.edu.udea.alamano.gestion_servicios.dominio.EstadoMensaje;

import java.time.OffsetDateTime;

public record MensajeDTO(
        Long idMensaje,
        Long idSolicitud,
        Long idRemitente,
        String nombreRemitente,
        String contenido,
        EstadoMensaje estadoMensaje,
        OffsetDateTime fechaEnvio
) {
}
