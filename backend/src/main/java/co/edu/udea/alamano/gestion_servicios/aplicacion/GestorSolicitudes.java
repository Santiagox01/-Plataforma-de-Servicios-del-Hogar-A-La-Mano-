package co.edu.udea.alamano.gestion_servicios.aplicacion;

import co.edu.udea.alamano.autenticacion_seguridad.dominio.Administrador;
import co.edu.udea.alamano.autenticacion_seguridad.dominio.Cliente;
import co.edu.udea.alamano.autenticacion_seguridad.dominio.Profesional;
import co.edu.udea.alamano.autenticacion_seguridad.dominio.RolUsuario;
import co.edu.udea.alamano.autenticacion_seguridad.infraestructura.AdministradorRepositorio;
import co.edu.udea.alamano.autenticacion_seguridad.infraestructura.ClienteRepositorio;
import co.edu.udea.alamano.autenticacion_seguridad.infraestructura.ProfesionalRepositorio;
import co.edu.udea.alamano.autenticacion_seguridad.infraestructura.RepositorioFactory;
import co.edu.udea.alamano.comun.almacenamiento.IAlmacenamientoArchivos;
import co.edu.udea.alamano.comun.excepcion.ExcepcionNegocio;
import co.edu.udea.alamano.comun.excepcion.ExcepcionNoAutorizado;
import co.edu.udea.alamano.comun.excepcion.ExcepcionRecursoNoEncontrado;
import co.edu.udea.alamano.comun.notificacion.ServicioNotificaciones;
import co.edu.udea.alamano.comun.validacion.ValidadorDatosComunes;
import co.edu.udea.alamano.cotizacion_tarificacion.dominio.Servicio;
import co.edu.udea.alamano.cotizacion_tarificacion.dominio.TarificadorContext;
import co.edu.udea.alamano.cotizacion_tarificacion.infraestructura.ServicioRepositorio;
import co.edu.udea.alamano.gestion_servicios.dominio.Chat;
import co.edu.udea.alamano.gestion_servicios.dominio.EstadoSolicitud;
import co.edu.udea.alamano.gestion_servicios.dominio.Solicitud;
import co.edu.udea.alamano.gestion_servicios.dto.ProfesionalResumenDTO;
import co.edu.udea.alamano.gestion_servicios.dto.RespuestaRechazoSolicitudDTO;
import co.edu.udea.alamano.gestion_servicios.dto.SolicitudDTO;
import co.edu.udea.alamano.gestion_servicios.infraestructura.ChatRepositorio;
import co.edu.udea.alamano.gestion_servicios.infraestructura.ISolicitudRepositorio;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Controlador de aplicacion para el ciclo de vida de solicitudes (patron GRASP Controller).
 */
@Service
public class GestorSolicitudes {

    private final ISolicitudRepositorio solicitudRepositorio;
    private final ClienteRepositorio clienteRepositorio;
    private final ProfesionalRepositorio profesionalRepositorio;
    private final AdministradorRepositorio administradorRepositorio;
    private final ServicioRepositorio servicioRepositorio;
    private final TarificadorContext tarificadorContext;
    private final ValidadorDatosComunes validador;
    private final IAlmacenamientoArchivos almacenamientoArchivos;
    private final ServicioNotificaciones servicioNotificaciones;
    private final CalculadorDistanciaHaversine calculadorDistancia;
    private final IConsultaReputacion consultaReputacion;
    private final ChatRepositorio chatRepositorio;
    private final IRegistradorDisputaSoporte registradorDisputaSoporte;
    private final IConsultaPagoSolicitud consultaPagoSolicitud;
    private final IConsultaReportesAbiertos consultaReportesAbiertos;

    public GestorSolicitudes(RepositorioFactory repositorioFactory,
                             ServicioRepositorio servicioRepositorio,
                             TarificadorContext tarificadorContext,
                             ValidadorDatosComunes validador,
                             IAlmacenamientoArchivos almacenamientoArchivos,
                             ServicioNotificaciones servicioNotificaciones,
                             CalculadorDistanciaHaversine calculadorDistancia,
                             IConsultaReputacion consultaReputacion,
                             ChatRepositorio chatRepositorio,
                             IRegistradorDisputaSoporte registradorDisputaSoporte,
                             IConsultaPagoSolicitud consultaPagoSolicitud,
                             IConsultaReportesAbiertos consultaReportesAbiertos) {
        this.solicitudRepositorio = repositorioFactory.crearSolicitudRepositorio();
        this.clienteRepositorio = repositorioFactory.crearClienteRepositorio();
        this.profesionalRepositorio = repositorioFactory.crearProfesionalRepositorio();
        this.administradorRepositorio = repositorioFactory.crearAdministradorRepositorio();
        this.servicioRepositorio = servicioRepositorio;
        this.tarificadorContext = tarificadorContext;
        this.validador = validador;
        this.almacenamientoArchivos = almacenamientoArchivos;
        this.servicioNotificaciones = servicioNotificaciones;
        this.calculadorDistancia = calculadorDistancia;
        this.consultaReputacion = consultaReputacion;
        this.chatRepositorio = chatRepositorio;
        this.registradorDisputaSoporte = registradorDisputaSoporte;
        this.consultaPagoSolicitud = consultaPagoSolicitud;
        this.consultaReportesAbiertos = consultaReportesAbiertos;
    }

    @Transactional
    public SolicitudDTO crearSolicitud(Long idCliente,
                                       Long idServicio,
                                       String descripcion,
                                       boolean esUrgente,
                                       List<MultipartFile> fotos) {
        if (descripcion == null || descripcion.isBlank()) {
            throw new ExcepcionNegocio("La descripción del problema es obligatoria.");
        }
        validador.validarListaFotosSolicitud(fotos);

        Cliente cliente = clienteRepositorio.findById(idCliente)
                .orElseThrow(() -> new ExcepcionRecursoNoEncontrado("Cliente no encontrado."));
        Servicio servicio = servicioRepositorio.findById(idServicio)
                .orElseThrow(() -> new ExcepcionRecursoNoEncontrado("Servicio no encontrado."));

        Profesional profesional = servicio.getProfesional();
        if (!profesional.esVisibleEnBusqueda()) {
            throw new ExcepcionNegocio("El profesional seleccionado no se encuentra verificado y disponible.");
        }
        if (profesional.getIdUsuario().equals(cliente.getIdUsuario())) {
            throw new ExcepcionNegocio("No puedes enviarte una solicitud a ti mismo.");
        }

        BigDecimal precioEstimado = servicio.calcularPrecioEstimado(esUrgente, tarificadorContext).precioEstimado();

        List<String> rutasFotos = new ArrayList<>();
        if (fotos != null) {
            for (MultipartFile foto : fotos) {
                if (foto != null && !foto.isEmpty()) {
                    rutasFotos.add(almacenamientoArchivos.guardarArchivoPrivado(foto, "solicitudes"));
                }
            }
        }

        Solicitud solicitud = new Solicitud(cliente, profesional, servicio, descripcion, rutasFotos, esUrgente, precioEstimado);
        Solicitud guardada = solicitudRepositorio.guardar(solicitud);

        servicioNotificaciones.notificar(
                profesional.getIdUsuario(),
                "Nueva solicitud de servicio",
                cliente.getNombre() + " solicitó atención de " + servicio.getCategoriaOficio().getNombre()
                        + (esUrgente ? " (URGENTE)." : "."),
                "SOLICITUD_NUEVA"
        );

        return mapearDTO(guardada);
    }

    @Transactional(readOnly = true)
    public List<SolicitudDTO> listarMisSolicitudes(Long idUsuario, RolUsuario rol) {
        List<Solicitud> lista = (rol == RolUsuario.PROFESIONAL)
                ? solicitudRepositorio.listarPorProfesional(idUsuario)
                : solicitudRepositorio.listarPorCliente(idUsuario);
        return lista.stream().map(this::mapearDTO).toList();
    }

    @Transactional(readOnly = true)
    public SolicitudDTO obtenerDetalle(Long idSolicitud, Long idUsuario) {
        Solicitud solicitud = buscarSolicitudDetalle(idSolicitud);
        if (!solicitud.esParticipante(idUsuario)) {
            throw new ExcepcionNoAutorizado("No tienes acceso a esta solicitud.");
        }
        return mapearDTO(solicitud);
    }

    @Transactional
    public SolicitudDTO aceptarSolicitud(Long idSolicitud, Long idProfesional) {
        Solicitud solicitud = buscarSolicitudDetalle(idSolicitud);
        verificarDuenoProfesional(solicitud, idProfesional);

        solicitud.aceptar();
        Solicitud guardada = solicitudRepositorio.guardar(solicitud);
        if (guardada.getChat() == null) {
            chatRepositorio.save(new Chat(guardada));
        }

        servicioNotificaciones.notificar(
                guardada.getCliente().getIdUsuario(),
                "Solicitud aceptada",
                guardada.getProfesional().getNombre() + " aceptó tu solicitud. El chat ya está habilitado.",
                "SOLICITUD_ACEPTADA"
        );

        return mapearDTO(guardada);
    }

    @Transactional
    public RespuestaRechazoSolicitudDTO rechazarSolicitud(Long idSolicitud, Long idProfesional, String motivo) {
        Solicitud solicitud = buscarSolicitudDetalle(idSolicitud);
        verificarDuenoProfesional(solicitud, idProfesional);

        solicitud.rechazar(motivo);
        Solicitud guardada = solicitudRepositorio.guardar(solicitud);

        List<ProfesionalResumenDTO> sugerencias = buscarProfesionalesSugeridos(guardada);
        String textoMotivo = guardada.getMotivoRechazo() != null ? " Motivo: " + guardada.getMotivoRechazo() : "";

        servicioNotificaciones.notificar(
                guardada.getCliente().getIdUsuario(),
                "Solicitud rechazada",
                guardada.getProfesional().getNombre() + " no pudo aceptar tu solicitud." + textoMotivo
                        + (sugerencias.isEmpty() ? "" : " Hay otros profesionales disponibles en tu categoría."),
                "SOLICITUD_RECHAZADA"
        );

        return new RespuestaRechazoSolicitudDTO(mapearDTO(guardada), sugerencias);
    }

    @Transactional
    public SolicitudDTO registrarPrecioFinal(Long idSolicitud, Long idProfesional, BigDecimal precioFinal) {
        Solicitud solicitud = buscarSolicitudDetalle(idSolicitud);
        verificarDuenoProfesional(solicitud, idProfesional);

        solicitud.registrarPrecioFinal(precioFinal);
        Solicitud guardada = solicitudRepositorio.guardar(solicitud);

        servicioNotificaciones.notificar(
                guardada.getCliente().getIdUsuario(),
                "Precio final registrado",
                guardada.getProfesional().getNombre() + " registró el precio final de $" + guardada.getPrecioFinal()
                        + ". Por favor confírmalo para finalizar el servicio.",
                "PRECIO_FINAL_REGISTRADO"
        );

        return mapearDTO(guardada);
    }

    @Transactional
    public SolicitudDTO confirmarPrecioFinal(Long idSolicitud, Long idCliente) {
        Solicitud solicitud = buscarSolicitudDetalle(idSolicitud);
        verificarDuenoCliente(solicitud, idCliente);

        solicitud.confirmarPrecioFinal();
        Solicitud guardada = solicitudRepositorio.guardar(solicitud);

        servicioNotificaciones.notificar(
                guardada.getProfesional().getIdUsuario(),
                "Precio final confirmado",
                guardada.getCliente().getNombre() + " confirmó el precio final ($" + guardada.getPrecioFinal()
                        + "). La solicitud pasó a estado Finalizada.",
                "SOLICITUD_FINALIZADA"
        );

        return mapearDTO(guardada);
    }

    @Transactional
    public SolicitudDTO rechazarPrecioFinal(Long idSolicitud, Long idCliente, String motivo) {
        Solicitud solicitud = buscarSolicitudDetalle(idSolicitud);
        verificarDuenoCliente(solicitud, idCliente);

        solicitud.rechazarPrecioFinal();
        Solicitud guardada = solicitudRepositorio.guardar(solicitud);
        String detalle = (motivo != null && !motivo.isBlank()) ? " Motivo del cliente: " + motivo.trim() : "";

        servicioNotificaciones.notificar(
                guardada.getProfesional().getIdUsuario(),
                "Precio final en disputa",
                guardada.getCliente().getNombre() + " rechazó el precio final propuesto." + detalle
                        + " El caso fue escalado a soporte.",
                "SOLICITUD_EN_DISPUTA"
        );

        for (Administrador admin : administradorRepositorio.findAll()) {
            servicioNotificaciones.notificar(
                    admin.getIdUsuario(),
                    "Disputa por precio final (Solicitud #" + guardada.getIdSolicitud() + ")",
                    "El cliente " + guardada.getCliente().getNombre() + " rechazó el monto de $"
                            + guardada.getPrecioFinal() + " registrado por " + guardada.getProfesional().getNombre() + "." + detalle,
                    "DISPUTA_SOPORTE"
            );
        }

        registradorDisputaSoporte.crearCasoPorDisputa(guardada, motivo);

        return mapearDTO(guardada);
    }

    public SolicitudDTO mapearDTO(Solicitud s) {
        List<String> urlsFotos = s.getFotos().stream()
                .map(almacenamientoArchivos::generarUrlFirmadaTemporal)
                .toList();

        Optional<IConsultaPagoSolicitud.ResumenPagoSolicitud> pagoOpt =
                consultaPagoSolicitud.consultarPagoActual(s.getIdSolicitud());
        boolean tieneReporteAbierto = consultaReportesAbiertos.existeReporteAbierto(s.getIdSolicitud());
        boolean puedePagar = s.getEstadoSolicitud() == EstadoSolicitud.FINALIZADA
                && s.isPrecioFinalConfirmado()
                && pagoOpt.map(IConsultaPagoSolicitud.ResumenPagoSolicitud::estaRechazado).orElse(true);
        boolean puedeLiberarCustodia = s.puedeLiberarCustodia(tieneReporteAbierto)
                && pagoOpt.map(IConsultaPagoSolicitud.ResumenPagoSolicitud::estaRetenido).orElse(false);

        return new SolicitudDTO(
                s.getIdSolicitud(),
                s.getCliente().getIdUsuario(),
                s.getCliente().getNombre(),
                s.getProfesional().getIdUsuario(),
                s.getProfesional().getNombre(),
                s.getServicio().getIdServicio(),
                s.getServicio().getCategoriaOficio().getIdCategoria(),
                s.getServicio().getCategoriaOficio().getNombre(),
                s.getDescripcion(),
                urlsFotos,
                s.getEstadoSolicitud(),
                s.getMotivoRechazo(),
                s.isEsUrgente(),
                s.getPrecioEstimado(),
                s.getPrecioFinal(),
                s.isPrecioFinalConfirmado(),
                s.chatHabilitado(),
                s.puedeResenarse(),
                s.getResena() != null,
                s.getFechaSolicitud(),
                s.getFechaFinalizacion(),
                pagoOpt.map(IConsultaPagoSolicitud.ResumenPagoSolicitud::idPago).orElse(null),
                pagoOpt.map(IConsultaPagoSolicitud.ResumenPagoSolicitud::estadoPago).orElse(null),
                puedePagar,
                puedeLiberarCustodia,
                tieneReporteAbierto
        );
    }

    private Solicitud buscarSolicitudDetalle(Long idSolicitud) {
        return solicitudRepositorio.buscarDetallePorId(idSolicitud)
                .orElseThrow(() -> new ExcepcionRecursoNoEncontrado("Solicitud no encontrada."));
    }

    private void verificarDuenoProfesional(Solicitud solicitud, Long idProfesional) {
        if (!solicitud.getProfesional().getIdUsuario().equals(idProfesional)) {
            throw new ExcepcionNoAutorizado("Esta solicitud corresponde a otro profesional.");
        }
    }

    private void verificarDuenoCliente(Solicitud solicitud, Long idCliente) {
        if (!solicitud.getCliente().getIdUsuario().equals(idCliente)) {
            throw new ExcepcionNoAutorizado("Esta solicitud corresponde a otro cliente.");
        }
    }

    private List<ProfesionalResumenDTO> buscarProfesionalesSugeridos(Solicitud solicitud) {
        Long idCategoria = solicitud.getServicio().getCategoriaOficio().getIdCategoria();
        Long idRechazante = solicitud.getProfesional().getIdUsuario();
        Double latCliente = solicitud.getCliente().getLatitud();
        Double lonCliente = solicitud.getCliente().getLongitud();

        return profesionalRepositorio.buscarElegiblesPorCategoria(idCategoria)
                .stream()
                .filter(p -> !p.getIdUsuario().equals(idRechazante))
                .filter(p -> p.esVisibleEnBusqueda() && p.getLatitud() != null && p.getLongitud() != null)
                .map(p -> {
                    double distancia = (latCliente != null && lonCliente != null)
                            ? calculadorDistancia.calcularKm(latCliente, lonCliente, p.getLatitud(), p.getLongitud())
                            : 0.0;
                    IConsultaReputacion.ResumenReputacion rep = consultaReputacion.obtenerResumen(p.getIdUsuario());
                    Servicio srv = p.getServicios().stream()
                            .filter(s -> s.getCategoriaOficio().getIdCategoria().equals(idCategoria))
                            .findFirst()
                            .orElse(null);
                    return new ProfesionalResumenDTO(
                            p.getIdUsuario(),
                            p.getNombre(),
                            p.getUbicacion(),
                            distancia,
                            rep.calificacionPromedio(),
                            rep.totalResenas(),
                            true,
                            "Verificado",
                            srv != null ? srv.getIdServicio() : null,
                            srv != null ? srv.getTarifaBase() : null,
                            srv != null ? srv.getPrecioMin() : null,
                            srv != null ? srv.getPrecioMax() : null
                    );
                })
                .sorted(Comparator.comparingDouble(ProfesionalResumenDTO::distanciaKm))
                .limit(5)
                .toList();
    }
}
