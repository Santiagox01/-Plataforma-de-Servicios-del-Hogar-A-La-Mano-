package co.edu.udea.alamano.moderacion_reportes.aplicacion;

import co.edu.udea.alamano.autenticacion_seguridad.dominio.Administrador;
import co.edu.udea.alamano.autenticacion_seguridad.dominio.RolUsuario;
import co.edu.udea.alamano.autenticacion_seguridad.dominio.Usuario;
import co.edu.udea.alamano.autenticacion_seguridad.infraestructura.AdministradorRepositorio;
import co.edu.udea.alamano.autenticacion_seguridad.infraestructura.RepositorioFactory;
import co.edu.udea.alamano.comun.excepcion.ExcepcionNegocio;
import co.edu.udea.alamano.comun.notificacion.ServicioNotificaciones;
import co.edu.udea.alamano.gestion_servicios.dominio.Solicitud;
import co.edu.udea.alamano.gestion_servicios.infraestructura.ISolicitudRepositorio;
import co.edu.udea.alamano.moderacion_reportes.dominio.MotivoReporte;
import co.edu.udea.alamano.moderacion_reportes.dominio.Reporte;
import co.edu.udea.alamano.moderacion_reportes.dto.CrearReporteDTO;
import co.edu.udea.alamano.moderacion_reportes.dto.MotivoReporteDTO;
import co.edu.udea.alamano.moderacion_reportes.dto.ReporteVistaReportadoDTO;
import co.edu.udea.alamano.moderacion_reportes.dto.ReporteVistaReportanteDTO;
import co.edu.udea.alamano.moderacion_reportes.infraestructura.ReporteRepositorio;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ServicioReportes {

    private final ReporteRepositorio reporteRepositorio;
    private final ISolicitudRepositorio solicitudRepositorio;
    private final AdministradorRepositorio administradorRepositorio;
    private final ServicioNotificaciones servicioNotificaciones;

    public ServicioReportes(ReporteRepositorio reporteRepositorio,
                            RepositorioFactory repositorioFactory,
                            ServicioNotificaciones servicioNotificaciones) {
        this.reporteRepositorio = reporteRepositorio;
        this.solicitudRepositorio = repositorioFactory.crearSolicitudRepositorio();
        this.administradorRepositorio = repositorioFactory.crearAdministradorRepositorio();
        this.servicioNotificaciones = servicioNotificaciones;
    }

    public List<MotivoReporteDTO> listarMotivosPorRol(RolUsuario rol) {
        return MotivoReporte.porRolReportante(rol).stream()
                .map(m -> new MotivoReporteDTO(m, m.getEtiqueta()))
                .toList();
    }

    @Transactional
    public ReporteVistaReportanteDTO crearReporte(Long idReportante, RolUsuario rolReportante, CrearReporteDTO dto) {
        if (dto == null || dto.idSolicitud() == null) {
            throw new ExcepcionNegocio("El reporte debe estar asociado a una solicitud existente.");
        }
        Solicitud solicitud = solicitudRepositorio.buscarDetallePorId(dto.idSolicitud())
                .orElseThrow(() -> new ExcepcionNegocio(
                        "La solicitud indicada no existe. El reporte debe asociarse a una solicitud existente."));

        if (!solicitud.esParticipante(idReportante)) {
            throw new ExcepcionNegocio("Solo puedes crear un reporte asociado a una solicitud en la que participes.");
        }
        if (dto.motivo() == null || !dto.motivo().aplicaParaRolReportante(rolReportante)) {
            throw new ExcepcionNegocio("Debes seleccionar un motivo de reporte válido para tu rol.");
        }
        if (dto.descripcion() == null || dto.descripcion().isBlank()) {
            throw new ExcepcionNegocio("La descripción detallada de lo ocurrido es obligatoria.");
        }

        boolean reportaCliente = solicitud.getCliente().getIdUsuario().equals(idReportante);
        Usuario reportante = reportaCliente ? solicitud.getCliente() : solicitud.getProfesional();
        Usuario reportado = reportaCliente ? solicitud.getProfesional() : solicitud.getCliente();

        // Reincidencia: si el usuario reportado ya acumula reportes previos, el caso entra con prioridad alta
        boolean esPrioritario = reporteRepositorio.countByReportadoIdUsuario(reportado.getIdUsuario()) > 0;

        Reporte reporte = new Reporte(solicitud, reportante, reportado, dto.motivo(), dto.descripcion(), esPrioritario);
        Reporte guardado = reporteRepositorio.save(reporte);

        servicioNotificaciones.notificar(
                reportante.getIdUsuario(),
                "Reporte recibido (#" + guardado.getIdReporte() + ")",
                "Tu reporte sobre la solicitud #" + solicitud.getIdSolicitud()
                        + " fue enviado a moderación. Tu identidad permanecerá confidencial.",
                "REPORTE_RECIBIDO"
        );

        String tipoAdmin = esPrioritario ? "REPORTE_PRIORITARIO" : "REPORTE_NUEVO";
        String prefijo = esPrioritario ? "[PRIORITARIO por reincidencia] " : "";
        for (Administrador admin : administradorRepositorio.findAll()) {
            servicioNotificaciones.notificar(
                    admin.getIdUsuario(),
                    prefijo + "Nuevo reporte en moderación (#" + guardado.getIdReporte() + ")",
                    "Motivo: " + guardado.getMotivo().getEtiqueta() + " (Solicitud #" + solicitud.getIdSolicitud() + ").",
                    tipoAdmin
            );
        }

        return mapearVistaReportante(guardado);
    }

    @Transactional(readOnly = true)
    public List<ReporteVistaReportanteDTO> listarEnviados(Long idUsuario) {
        return reporteRepositorio.listarPorReportante(idUsuario)
                .stream()
                .map(this::mapearVistaReportante)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ReporteVistaReportadoDTO> listarRecibidos(Long idUsuario) {
        return reporteRepositorio.listarPorReportado(idUsuario)
                .stream()
                .map(this::mapearVistaReportado)
                .toList();
    }

    private ReporteVistaReportanteDTO mapearVistaReportante(Reporte r) {
        return new ReporteVistaReportanteDTO(
                r.getIdReporte(),
                r.getSolicitud().getIdSolicitud(),
                r.getSolicitud().getServicio().getCategoriaOficio().getNombre(),
                r.getReportado().getIdUsuario(),
                r.getReportado().getNombre(),
                r.getMotivo(),
                r.getMotivo().getEtiqueta(),
                r.getDescripcion(),
                r.isEsPrioritario(),
                r.getEstadoReporte(),
                r.getAccionTomada(),
                r.getJustificacionResolucion(),
                r.getFechaReporte(),
                r.getFechaResolucion()
        );
    }

    private ReporteVistaReportadoDTO mapearVistaReportado(Reporte r) {
        return new ReporteVistaReportadoDTO(
                r.getIdReporte(),
                r.getSolicitud().getIdSolicitud(),
                r.getSolicitud().getServicio().getCategoriaOficio().getNombre(),
                r.getMotivo(),
                r.getMotivo().getEtiqueta(),
                r.getEstadoReporte(),
                r.getAccionTomada(),
                r.getJustificacionResolucion(),
                r.getFechaReporte(),
                r.getFechaResolucion()
        );
    }
}
