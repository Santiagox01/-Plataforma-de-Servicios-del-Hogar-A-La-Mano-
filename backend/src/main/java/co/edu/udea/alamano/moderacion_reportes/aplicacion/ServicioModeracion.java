package co.edu.udea.alamano.moderacion_reportes.aplicacion;

import co.edu.udea.alamano.autenticacion_seguridad.dominio.Administrador;
import co.edu.udea.alamano.autenticacion_seguridad.dominio.RolUsuario;
import co.edu.udea.alamano.autenticacion_seguridad.dominio.Usuario;
import co.edu.udea.alamano.autenticacion_seguridad.infraestructura.AdministradorRepositorio;
import co.edu.udea.alamano.autenticacion_seguridad.infraestructura.IUsuarioRepositorio;
import co.edu.udea.alamano.autenticacion_seguridad.infraestructura.RepositorioFactory;
import co.edu.udea.alamano.comun.auditoria.IComandoAdministrativo;
import co.edu.udea.alamano.comun.auditoria.ServicioAuditoria;
import co.edu.udea.alamano.comun.configuracion.PropiedadesApp;
import co.edu.udea.alamano.comun.excepcion.ExcepcionNegocio;
import co.edu.udea.alamano.comun.excepcion.ExcepcionRecursoNoEncontrado;
import co.edu.udea.alamano.comun.notificacion.ServicioNotificaciones;
import co.edu.udea.alamano.gestion_servicios.aplicacion.IConsultaPagoSolicitud;
import co.edu.udea.alamano.moderacion_reportes.dominio.AccionModeracion;
import co.edu.udea.alamano.moderacion_reportes.dominio.DecisionPagoModeracion;
import co.edu.udea.alamano.moderacion_reportes.dominio.EstadoReporte;
import co.edu.udea.alamano.moderacion_reportes.dominio.Reporte;
import co.edu.udea.alamano.moderacion_reportes.dominio.sancion.ISancionable;
import co.edu.udea.alamano.moderacion_reportes.dto.ReporteAdminDTO;
import co.edu.udea.alamano.moderacion_reportes.dto.ResolverReporteDTO;
import co.edu.udea.alamano.moderacion_reportes.infraestructura.ReporteRepositorio;
import co.edu.udea.alamano.pagos.aplicacion.ServicioPagos;
import org.hibernate.Hibernate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class ServicioModeracion {

    private final ReporteRepositorio reporteRepositorio;
    private final IUsuarioRepositorio usuarioRepositorio;
    private final AdministradorRepositorio administradorRepositorio;
    private final Map<RolUsuario, ISancionable> sancionesPorRol;
    private final IConsultaPagoSolicitud consultaPagoSolicitud;
    private final ServicioPagos servicioPagos;
    private final ServicioAuditoria servicioAuditoria;
    private final ServicioNotificaciones servicioNotificaciones;
    private final PropiedadesApp propiedades;

    public ServicioModeracion(ReporteRepositorio reporteRepositorio,
                              RepositorioFactory repositorioFactory,
                              List<ISancionable> sancionadores,
                              IConsultaPagoSolicitud consultaPagoSolicitud,
                              ServicioPagos servicioPagos,
                              ServicioAuditoria servicioAuditoria,
                              ServicioNotificaciones servicioNotificaciones,
                              PropiedadesApp propiedades) {
        this.reporteRepositorio = reporteRepositorio;
        this.usuarioRepositorio = repositorioFactory.crearUsuarioRepositorio();
        this.administradorRepositorio = repositorioFactory.crearAdministradorRepositorio();
        this.sancionesPorRol = sancionadores.stream()
                .collect(Collectors.toMap(ISancionable::getRolSoportado, Function.identity()));
        this.consultaPagoSolicitud = consultaPagoSolicitud;
        this.servicioPagos = servicioPagos;
        this.servicioAuditoria = servicioAuditoria;
        this.servicioNotificaciones = servicioNotificaciones;
        this.propiedades = propiedades;
    }

    @Transactional(readOnly = true)
    public List<ReporteAdminDTO> listarCola(EstadoReporte estado) {
        List<Reporte> reportes = (estado != null)
                ? reporteRepositorio.listarPorEstadoOrdenCola(estado)
                : reporteRepositorio.listarTodosOrdenCola();
        return reportes.stream().map(this::mapearAdminDTO).toList();
    }

    @Transactional
    public ReporteAdminDTO resolverReporte(Long idReporte, Long idAdmin, ResolverReporteDTO dto) {
        IComandoAdministrativo<ReporteAdminDTO> comando = () -> {
            Reporte reporte = reporteRepositorio.buscarPorIdConDetalles(idReporte)
                    .orElseThrow(() -> new ExcepcionRecursoNoEncontrado("Reporte no encontrado."));
            Administrador admin = administradorRepositorio.findById(idAdmin)
                    .orElseThrow(() -> new ExcepcionRecursoNoEncontrado("Administrador no encontrado."));

            Integer diasSuspension = dto.accion() == AccionModeracion.SUSPENSION_TEMPORAL
                    ? (dto.diasSuspension() != null
                    ? dto.diasSuspension()
                    : propiedades.getModeracion().getDiasSuspensionDefecto())
                    : null;
            DecisionPagoModeracion decisionPago = dto.decisionPago() != null
                    ? dto.decisionPago()
                    : DecisionPagoModeracion.SIN_CAMBIO;

            reporte.resolver(admin, dto.accion(), dto.justificacion(), diasSuspension, decisionPago);

            String resumenSancion = "Caso descartado por falta de mérito.";
            Usuario reportado = Hibernate.unproxy(reporte.getReportado(), Usuario.class);
            if (dto.accion() != AccionModeracion.DESCARTAR) {
                resumenSancion = aplicarSancionPolimorfica(reportado, dto.accion(), dto.justificacion(), diasSuspension);
                usuarioRepositorio.guardar(reportado);
            }

            Long idSolicitud = reporte.getSolicitud().getIdSolicitud();
            if (decisionPago == DecisionPagoModeracion.REEMBOLSAR) {
                servicioPagos.reembolsarPorModeracion(idSolicitud, idAdmin, dto.justificacion());
            } else if (decisionPago == DecisionPagoModeracion.LIBERAR) {
                servicioPagos.liberarPorModeracion(idSolicitud, idAdmin, dto.justificacion());
            }

            Reporte guardado = reporteRepositorio.save(reporte);

            servicioAuditoria.registrar(
                    admin.getIdUsuario(),
                    "RESOLVER_REPORTE_" + guardado.getAccionTomada().name(),
                    "REPORTE",
                    guardado.getIdReporte(),
                    "Justificación: " + guardado.getJustificacionResolucion()
                            + " | Decisión pago: " + guardado.getDecisionPago()
                            + " | " + resumenSancion
            );

            notificarResultado(guardado, resumenSancion);
            return mapearAdminDTO(guardado);
        };
        return comando.ejecutar();
    }

    private String aplicarSancionPolimorfica(Usuario reportado,
                                              AccionModeracion accion,
                                              String justificacion,
                                              Integer diasSuspension) {
        ISancionable estrategiaSancion = sancionesPorRol.get(reportado.getRol());
        if (estrategiaSancion == null) {
            throw new ExcepcionNegocio("No hay política de sanción configurada para el rol " + reportado.getRol() + ".");
        }
        return switch (accion) {
            case ADVERTENCIA -> estrategiaSancion.aplicarAdvertencia(reportado, justificacion);
            case SUSPENSION_TEMPORAL -> estrategiaSancion.aplicarSuspensionTemporal(
                    reportado, OffsetDateTime.now().plusDays(diasSuspension), justificacion);
            case DESACTIVACION -> estrategiaSancion.aplicarDesactivacion(reportado, justificacion);
            case DESCARTAR -> "Caso descartado sin sanción.";
        };
    }

    private void notificarResultado(Reporte r, String resumenSancion) {
        Long idReportado = r.getReportado().getIdUsuario();
        Long idReportante = r.getReportante().getIdUsuario();
        Long idSolicitud = r.getSolicitud().getIdSolicitud();

        if (r.getAccionTomada() == AccionModeracion.DESCARTAR) {
            // Sin merito: se notifica a ambas partes sin revelar la identidad del reportante
            servicioNotificaciones.notificar(
                    idReportado,
                    "Reporte cerrado sin sanción",
                    "La revisión sobre la solicitud #" + idSolicitud + " cerró como Sin mérito. Justificación: "
                            + r.getJustificacionResolucion(),
                    "REPORTE_DESCARTADO"
            );
            servicioNotificaciones.notificar(
                    idReportante,
                    "Reporte resuelto (Sin mérito)",
                    "Tu reporte #" + r.getIdReporte() + " fue cerrado como Sin mérito. Justificación: "
                            + r.getJustificacionResolucion(),
                    "REPORTE_DESCARTADO"
            );
            return;
        }

        // Sancion: se notifica al reportado sin exponer al reportante
        servicioNotificaciones.notificar(
                idReportado,
                "Resolución de moderación aplicada",
                "Se aplicó la medida " + r.getAccionTomada() + " respecto a la solicitud #" + idSolicitud
                        + ". " + resumenSancion,
                "SANCION_APLICADA"
        );
        servicioNotificaciones.notificar(
                idReportante,
                "Tu reporte fue resuelto",
                "El caso #" + r.getIdReporte() + " fue resuelto con la acción " + r.getAccionTomada()
                        + ". Justificación: " + r.getJustificacionResolucion(),
                "REPORTE_RESUELTO"
        );
    }

    private ReporteAdminDTO mapearAdminDTO(Reporte r) {
        long prevReportante = Math.max(0L, reporteRepositorio.countByReportanteIdUsuario(r.getReportante().getIdUsuario()) - 1L);
        long prevReportado = Math.max(0L, reporteRepositorio.countByReportadoIdUsuario(r.getReportado().getIdUsuario()) - 1L);
        Optional<IConsultaPagoSolicitud.ResumenPagoSolicitud> pagoOpt =
                consultaPagoSolicitud.consultarPagoActual(r.getSolicitud().getIdSolicitud());

        return new ReporteAdminDTO(
                r.getIdReporte(),
                r.getSolicitud().getIdSolicitud(),
                r.getSolicitud().getServicio().getCategoriaOficio().getNombre(),
                r.getReportante().getIdUsuario(),
                r.getReportante().getNombre(),
                r.getReportante().getRol(),
                prevReportante,
                r.getReportado().getIdUsuario(),
                r.getReportado().getNombre(),
                r.getReportado().getRol(),
                prevReportado,
                r.getMotivo(),
                r.getMotivo().getEtiqueta(),
                r.getDescripcion(),
                r.isEsPrioritario(),
                r.getEstadoReporte(),
                r.getAccionTomada(),
                r.getJustificacionResolucion(),
                r.getDiasSuspension(),
                r.getDecisionPago(),
                pagoOpt.map(IConsultaPagoSolicitud.ResumenPagoSolicitud::estadoPago).orElse(null),
                pagoOpt.map(IConsultaPagoSolicitud.ResumenPagoSolicitud::estaRetenido).orElse(false),
                r.getIdAdminResolutor(),
                r.getFechaReporte(),
                r.getFechaResolucion()
        );
    }
}
