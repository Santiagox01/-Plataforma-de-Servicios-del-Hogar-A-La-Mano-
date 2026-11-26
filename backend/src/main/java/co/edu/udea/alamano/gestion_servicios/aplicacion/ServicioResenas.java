package co.edu.udea.alamano.gestion_servicios.aplicacion;

import co.edu.udea.alamano.autenticacion_seguridad.infraestructura.RepositorioFactory;
import co.edu.udea.alamano.comun.excepcion.ExcepcionConflicto;
import co.edu.udea.alamano.comun.excepcion.ExcepcionNoAutorizado;
import co.edu.udea.alamano.comun.excepcion.ExcepcionRecursoNoEncontrado;
import co.edu.udea.alamano.comun.notificacion.ServicioNotificaciones;
import co.edu.udea.alamano.gestion_servicios.dominio.Resena;
import co.edu.udea.alamano.gestion_servicios.dominio.Solicitud;
import co.edu.udea.alamano.gestion_servicios.dto.CrearResenaDTO;
import co.edu.udea.alamano.gestion_servicios.dto.ResenaDTO;
import co.edu.udea.alamano.gestion_servicios.infraestructura.ISolicitudRepositorio;
import co.edu.udea.alamano.gestion_servicios.infraestructura.ResenaRepositorio;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ServicioResenas {

    private final ISolicitudRepositorio solicitudRepositorio;
    private final ResenaRepositorio resenaRepositorio;
    private final IConsultaReputacion consultaReputacion;
    private final ServicioNotificaciones servicioNotificaciones;

    public ServicioResenas(RepositorioFactory repositorioFactory,
                           ResenaRepositorio resenaRepositorio,
                           IConsultaReputacion consultaReputacion,
                           ServicioNotificaciones servicioNotificaciones) {
        this.solicitudRepositorio = repositorioFactory.crearSolicitudRepositorio();
        this.resenaRepositorio = resenaRepositorio;
        this.consultaReputacion = consultaReputacion;
        this.servicioNotificaciones = servicioNotificaciones;
    }

    @Transactional
    public ResenaDTO crearResena(Long idSolicitud, Long idCliente, CrearResenaDTO dto) {
        Solicitud solicitud = solicitudRepositorio.buscarDetallePorId(idSolicitud)
                .orElseThrow(() -> new ExcepcionRecursoNoEncontrado("Solicitud no encontrada."));

        if (!solicitud.getCliente().getIdUsuario().equals(idCliente)) {
            throw new ExcepcionNoAutorizado("Solo el cliente titular de la solicitud puede calificar este servicio.");
        }
        if (resenaRepositorio.existsBySolicitudIdSolicitud(idSolicitud)) {
            throw new ExcepcionConflicto("Esta solicitud ya fue calificada.");
        }

        Resena resena = solicitud.resenar(dto.estrellas(), dto.comentario());
        Resena guardada = resenaRepositorio.saveAndFlush(resena);

        IConsultaReputacion.ResumenReputacion reputacionActualizada =
                consultaReputacion.obtenerResumen(solicitud.getProfesional().getIdUsuario());

        servicioNotificaciones.notificar(
                solicitud.getProfesional().getIdUsuario(),
                "Nueva calificación recibida",
                solicitud.getCliente().getNombre() + " calificó tu servicio con " + guardada.getEstrellas() + " estrellas.",
                "NUEVA_RESENA"
        );

        return new ResenaDTO(
                guardada.getIdResena(),
                idSolicitud,
                solicitud.getProfesional().getIdUsuario(),
                guardada.getEstrellas(),
                guardada.getComentario(),
                guardada.getFechaResena(),
                reputacionActualizada.calificacionPromedio(),
                reputacionActualizada.totalResenas()
        );
    }
}
