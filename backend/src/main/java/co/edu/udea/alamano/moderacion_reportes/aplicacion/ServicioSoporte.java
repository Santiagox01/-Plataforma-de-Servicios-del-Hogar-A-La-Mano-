package co.edu.udea.alamano.moderacion_reportes.aplicacion;

import co.edu.udea.alamano.autenticacion_seguridad.dominio.Administrador;
import co.edu.udea.alamano.autenticacion_seguridad.dominio.Usuario;
import co.edu.udea.alamano.autenticacion_seguridad.infraestructura.AdministradorRepositorio;
import co.edu.udea.alamano.autenticacion_seguridad.infraestructura.IUsuarioRepositorio;
import co.edu.udea.alamano.autenticacion_seguridad.infraestructura.RepositorioFactory;
import co.edu.udea.alamano.comun.auditoria.IComandoAdministrativo;
import co.edu.udea.alamano.comun.auditoria.ServicioAuditoria;
import co.edu.udea.alamano.comun.excepcion.ExcepcionNoAutorizado;
import co.edu.udea.alamano.comun.excepcion.ExcepcionRecursoNoEncontrado;
import co.edu.udea.alamano.comun.notificacion.ServicioNotificaciones;
import co.edu.udea.alamano.gestion_servicios.aplicacion.IRegistradorDisputaSoporte;
import co.edu.udea.alamano.gestion_servicios.dominio.Solicitud;
import co.edu.udea.alamano.gestion_servicios.infraestructura.ISolicitudRepositorio;
import co.edu.udea.alamano.moderacion_reportes.dominio.CasoSoporte;
import co.edu.udea.alamano.moderacion_reportes.dominio.EstadoCasoSoporte;
import co.edu.udea.alamano.moderacion_reportes.dto.CasoSoporteDTO;
import co.edu.udea.alamano.moderacion_reportes.dto.CrearCasoSoporteDTO;
import co.edu.udea.alamano.moderacion_reportes.dto.ResponderCasoSoporteDTO;
import co.edu.udea.alamano.moderacion_reportes.infraestructura.CasoSoporteRepositorio;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class ServicioSoporte implements IRegistradorDisputaSoporte {

    private final CasoSoporteRepositorio casoSoporteRepositorio;
    private final IUsuarioRepositorio usuarioRepositorio;
    private final AdministradorRepositorio administradorRepositorio;
    private final ISolicitudRepositorio solicitudRepositorio;
    private final ServicioNotificaciones servicioNotificaciones;
    private final ServicioAuditoria servicioAuditoria;

    public ServicioSoporte(CasoSoporteRepositorio casoSoporteRepositorio,
                           RepositorioFactory repositorioFactory,
                           ServicioNotificaciones servicioNotificaciones,
                           ServicioAuditoria servicioAuditoria) {
        this.casoSoporteRepositorio = casoSoporteRepositorio;
        this.usuarioRepositorio = repositorioFactory.crearUsuarioRepositorio();
        this.administradorRepositorio = repositorioFactory.crearAdministradorRepositorio();
        this.solicitudRepositorio = repositorioFactory.crearSolicitudRepositorio();
        this.servicioNotificaciones = servicioNotificaciones;
        this.servicioAuditoria = servicioAuditoria;
    }

    @Transactional
    public CasoSoporteDTO crearCaso(Long idUsuario, CrearCasoSoporteDTO dto) {
        Usuario usuario = usuarioRepositorio.buscarPorId(idUsuario)
                .orElseThrow(() -> new ExcepcionRecursoNoEncontrado("Usuario no encontrado."));

        Solicitud solicitud = null;
        if (dto.idSolicitud() != null) {
            solicitud = solicitudRepositorio.buscarDetallePorId(dto.idSolicitud())
                    .orElseThrow(() -> new ExcepcionRecursoNoEncontrado("Solicitud asociada no encontrada."));
            if (!solicitud.esParticipante(idUsuario)) {
                throw new ExcepcionNoAutorizado("Solo puedes asociar una solicitud en la que participes.");
            }
        }

        CasoSoporte caso = new CasoSoporte(usuario, solicitud, dto.asunto(), dto.descripcion());
        CasoSoporte guardado = casoSoporteRepositorio.save(caso);

        servicioNotificaciones.notificar(
                usuario.getIdUsuario(),
                "Ticket de soporte creado (" + guardado.getNumeroCaso() + ")",
                "Recibimos tu solicitud de soporte: \"" + guardado.getAsunto()
                        + "\". Número de caso: " + guardado.getNumeroCaso() + ".",
                "SOPORTE_CREADO"
        );

        for (Administrador admin : administradorRepositorio.findAll()) {
            servicioNotificaciones.notificar(
                    admin.getIdUsuario(),
                    "Nuevo caso de soporte (" + guardado.getNumeroCaso() + ")",
                    usuario.getNombre() + " abrió el ticket " + guardado.getNumeroCaso() + ": " + guardado.getAsunto(),
                    "SOPORTE_NUEVO"
            );
        }

        return mapearDTO(guardado);
    }

    @Override
    @Transactional
    public String crearCasoPorDisputa(Solicitud solicitud, String motivo) {
        Optional<CasoSoporte> existente = casoSoporteRepositorio
                .findFirstBySolicitudIdSolicitudAndEstadoCaso(solicitud.getIdSolicitud(), EstadoCasoSoporte.ABIERTO);
        if (existente.isPresent()) {
            return existente.get().getNumeroCaso();
        }
        String detalleMotivo = (motivo != null && !motivo.isBlank())
                ? " Motivo del cliente: " + motivo.trim()
                : "";
        CasoSoporte casoDisputa = new CasoSoporte(
                solicitud.getCliente(),
                solicitud,
                "Disputa por precio final — Solicitud #" + solicitud.getIdSolicitud(),
                "El cliente rechazó el precio final de $" + solicitud.getPrecioFinal()
                        + " COP propuesto por " + solicitud.getProfesional().getNombre() + "." + detalleMotivo
        );
        CasoSoporte guardado = casoSoporteRepositorio.save(casoDisputa);
        return guardado.getNumeroCaso();
    }

    @Transactional(readOnly = true)
    public List<CasoSoporteDTO> listarMisCasos(Long idUsuario) {
        return casoSoporteRepositorio.listarPorUsuario(idUsuario)
                .stream()
                .map(this::mapearDTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<CasoSoporteDTO> listarTodosAdmin() {
        return casoSoporteRepositorio.listarTodosAdmin()
                .stream()
                .map(this::mapearDTO)
                .toList();
    }

    @Transactional
    public CasoSoporteDTO responderCaso(Long idCaso, Long idAdmin, ResponderCasoSoporteDTO dto) {
        IComandoAdministrativo<CasoSoporteDTO> comando = () -> {
            CasoSoporte caso = casoSoporteRepositorio.buscarPorIdConDetalles(idCaso)
                    .orElseThrow(() -> new ExcepcionRecursoNoEncontrado("Caso de soporte no encontrado."));
            Administrador admin = administradorRepositorio.findById(idAdmin)
                    .orElseThrow(() -> new ExcepcionRecursoNoEncontrado("Administrador no encontrado."));

            boolean cerrar = Boolean.TRUE.equals(dto.cerrar());
            caso.responder(admin, dto.respuesta(), cerrar);
            CasoSoporte guardado = casoSoporteRepositorio.save(caso);

            servicioAuditoria.registrar(
                    admin.getIdUsuario(),
                    "RESPONDER_CASO_SOPORTE",
                    "CASO_SOPORTE",
                    guardado.getIdCaso(),
                    "Ticket " + guardado.getNumeroCaso() + " (" + guardado.getEstadoCaso() + "): "
                            + guardado.getRespuestaAdmin()
            );

            servicioNotificaciones.notificar(
                    guardado.getUsuario().getIdUsuario(),
                    "Respuesta a tu caso " + guardado.getNumeroCaso(),
                    "Soporte respondió: " + guardado.getRespuestaAdmin(),
                    "SOPORTE_RESPONDIDO"
            );

            return mapearDTO(guardado);
        };
        return comando.ejecutar();
    }

    private CasoSoporteDTO mapearDTO(CasoSoporte c) {
        return new CasoSoporteDTO(
                c.getIdCaso(),
                c.getNumeroCaso(),
                c.getUsuario().getIdUsuario(),
                c.getUsuario().getNombre(),
                c.getSolicitud() != null ? c.getSolicitud().getIdSolicitud() : null,
                c.getAsunto(),
                c.getDescripcion(),
                c.getEstadoCaso(),
                c.getRespuestaAdmin(),
                c.getIdAdminRespondedor(),
                c.getFechaCreacion(),
                c.getFechaRespuesta()
        );
    }
}
