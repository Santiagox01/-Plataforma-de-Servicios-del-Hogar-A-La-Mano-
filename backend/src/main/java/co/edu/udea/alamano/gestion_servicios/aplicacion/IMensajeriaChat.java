package co.edu.udea.alamano.gestion_servicios.aplicacion;

import co.edu.udea.alamano.gestion_servicios.dto.MensajeDTO;

import java.util.List;

/**
 * Puerto de mensajeria para desacoplar la implementacion REST+polling
 * de una migracion futura a WebSocket (STOMP).
 */
public interface IMensajeriaChat {

    MensajeDTO enviarMensaje(Long idSolicitud, Long idRemitente, String contenido);

    List<MensajeDTO> listarMensajes(Long idSolicitud, Long idUsuario);

    MensajeDTO marcarComoLeido(Long idSolicitud, Long idMensaje, Long idUsuario);
}
