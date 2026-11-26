package co.edu.udea.alamano.gestion_servicios.aplicacion;

import co.edu.udea.alamano.autenticacion_seguridad.dominio.Usuario;
import co.edu.udea.alamano.autenticacion_seguridad.infraestructura.IUsuarioRepositorio;
import co.edu.udea.alamano.autenticacion_seguridad.infraestructura.RepositorioFactory;
import co.edu.udea.alamano.comun.excepcion.ExcepcionNegocio;
import co.edu.udea.alamano.comun.excepcion.ExcepcionNoAutorizado;
import co.edu.udea.alamano.comun.excepcion.ExcepcionRecursoNoEncontrado;
import co.edu.udea.alamano.gestion_servicios.dominio.Chat;
import co.edu.udea.alamano.gestion_servicios.dominio.Mensaje;
import co.edu.udea.alamano.gestion_servicios.dominio.Solicitud;
import co.edu.udea.alamano.gestion_servicios.dto.MensajeDTO;
import co.edu.udea.alamano.gestion_servicios.infraestructura.ChatRepositorio;
import co.edu.udea.alamano.gestion_servicios.infraestructura.ISolicitudRepositorio;
import co.edu.udea.alamano.gestion_servicios.infraestructura.MensajeRepositorio;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ServicioMensajeriaRest implements IMensajeriaChat {

    private final ISolicitudRepositorio solicitudRepositorio;
    private final ChatRepositorio chatRepositorio;
    private final MensajeRepositorio mensajeRepositorio;
    private final IUsuarioRepositorio usuarioRepositorio;

    public ServicioMensajeriaRest(RepositorioFactory repositorioFactory,
                                  ChatRepositorio chatRepositorio,
                                  MensajeRepositorio mensajeRepositorio) {
        this.solicitudRepositorio = repositorioFactory.crearSolicitudRepositorio();
        this.usuarioRepositorio = repositorioFactory.crearUsuarioRepositorio();
        this.chatRepositorio = chatRepositorio;
        this.mensajeRepositorio = mensajeRepositorio;
    }

    @Override
    @Transactional
    public MensajeDTO enviarMensaje(Long idSolicitud, Long idRemitente, String contenido) {
        Solicitud solicitud = validarAccesoYChatHabilitado(idSolicitud, idRemitente);
        Usuario remitente = usuarioRepositorio.buscarPorId(idRemitente)
                .orElseThrow(() -> new ExcepcionRecursoNoEncontrado("Usuario no encontrado."));

        Chat chat = chatRepositorio.findBySolicitudIdSolicitud(idSolicitud)
                .orElseGet(() -> chatRepositorio.save(new Chat(solicitud)));

        Mensaje mensaje = chat.enviarMensaje(remitente, contenido);
        Mensaje guardado = mensajeRepositorio.save(mensaje);
        return mapearDTO(idSolicitud, guardado);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MensajeDTO> listarMensajes(Long idSolicitud, Long idUsuario) {
        validarAccesoYChatHabilitado(idSolicitud, idUsuario);
        return chatRepositorio.findBySolicitudIdSolicitud(idSolicitud)
                .map(chat -> mensajeRepositorio.listarPorChat(chat.getIdChat())
                        .stream()
                        .map(m -> mapearDTO(idSolicitud, m))
                        .toList())
                .orElse(List.of());
    }

    @Override
    @Transactional
    public MensajeDTO marcarComoLeido(Long idSolicitud, Long idMensaje, Long idUsuario) {
        validarAccesoYChatHabilitado(idSolicitud, idUsuario);
        Mensaje mensaje = mensajeRepositorio.buscarPorIdYSolicitud(idMensaje, idSolicitud)
                .orElseThrow(() -> new ExcepcionRecursoNoEncontrado("Mensaje no encontrado en esta solicitud."));

        if (mensaje.getRemitente().getIdUsuario().equals(idUsuario)) {
            throw new ExcepcionNegocio("Solo el destinatario puede marcar el mensaje como leído.");
        }

        mensaje.marcarComoLeido();
        Mensaje guardado = mensajeRepositorio.save(mensaje);
        return mapearDTO(idSolicitud, guardado);
    }

    private Solicitud validarAccesoYChatHabilitado(Long idSolicitud, Long idUsuario) {
        Solicitud solicitud = solicitudRepositorio.buscarDetallePorId(idSolicitud)
                .orElseThrow(() -> new ExcepcionRecursoNoEncontrado("Solicitud no encontrada."));
        if (!solicitud.esParticipante(idUsuario)) {
            throw new ExcepcionNoAutorizado("No participas en esta solicitud.");
        }
        if (!solicitud.chatHabilitado()) {
            throw new ExcepcionNegocio("El chat solo está habilitado cuando la solicitud se encuentra Aceptada o en una fase posterior.");
        }
        return solicitud;
    }

    private MensajeDTO mapearDTO(Long idSolicitud, Mensaje m) {
        return new MensajeDTO(
                m.getIdMensaje(),
                idSolicitud,
                m.getRemitente().getIdUsuario(),
                m.getRemitente().getNombre(),
                m.getContenido(),
                m.getEstadoMensaje(),
                m.getFechaEnvio()
        );
    }
}
