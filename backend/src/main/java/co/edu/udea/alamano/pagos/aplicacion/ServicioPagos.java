package co.edu.udea.alamano.pagos.aplicacion;

import co.edu.udea.alamano.autenticacion_seguridad.infraestructura.RepositorioFactory;
import co.edu.udea.alamano.comun.auditoria.ServicioAuditoria;
import co.edu.udea.alamano.comun.configuracion.PropiedadesApp;
import co.edu.udea.alamano.comun.configuracion.TarifaConfig;
import co.edu.udea.alamano.comun.excepcion.ExcepcionConflicto;
import co.edu.udea.alamano.comun.excepcion.ExcepcionNegocio;
import co.edu.udea.alamano.comun.excepcion.ExcepcionNoAutorizado;
import co.edu.udea.alamano.comun.excepcion.ExcepcionRecursoNoEncontrado;
import co.edu.udea.alamano.comun.notificacion.ServicioNotificaciones;
import co.edu.udea.alamano.gestion_servicios.aplicacion.IConsultaReportesAbiertos;
import co.edu.udea.alamano.gestion_servicios.dominio.EstadoSolicitud;
import co.edu.udea.alamano.gestion_servicios.dominio.Solicitud;
import co.edu.udea.alamano.gestion_servicios.infraestructura.ISolicitudRepositorio;
import co.edu.udea.alamano.pagos.dominio.EstadoPago;
import co.edu.udea.alamano.pagos.dominio.IPasarelaPagoRedireccion;
import co.edu.udea.alamano.pagos.dominio.IPasarelaPagos;
import co.edu.udea.alamano.pagos.dominio.MetodoPago;
import co.edu.udea.alamano.pagos.dominio.Pago;
import co.edu.udea.alamano.pagos.dominio.TipoMetodoPago;
import co.edu.udea.alamano.pagos.dto.PagoDTO;
import co.edu.udea.alamano.pagos.dto.RespuestaInicioPagoDTO;
import co.edu.udea.alamano.pagos.dto.ResumenPagoDTO;
import co.edu.udea.alamano.pagos.infraestructura.MetodoPagoRepositorio;
import co.edu.udea.alamano.pagos.infraestructura.PagoRepositorio;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

@Service
public class ServicioPagos {

    private static final String PREFIJO_REFERENCIA_IDEMPOTENCIA = "PAY-SOL-";
    private static final List<EstadoPago> ESTADOS_PAGO_CONFIRMADO = List.of(
            EstadoPago.RETENIDO,
            EstadoPago.LIBERADO,
            EstadoPago.REEMBOLSADO
    );
    private static final Pattern PATRON_APP_REDIRECT = Pattern.compile("^(alamano|exp)://[A-Za-z0-9._~:/?#\\[\\]@!$&'()*+,;=%-]+$");

    private final PagoRepositorio pagoRepositorio;
    private final MetodoPagoRepositorio metodoPagoRepositorio;
    private final ISolicitudRepositorio solicitudRepositorio;
    private final IPasarelaPagos pasarelaPagos;
    private final TarifaConfig tarifaConfig;
    private final IConsultaReportesAbiertos consultaReportesAbiertos;
    private final ServicioNotificaciones servicioNotificaciones;
    private final ServicioAuditoria servicioAuditoria;
    private final PropiedadesApp propiedadesApp;

    public ServicioPagos(PagoRepositorio pagoRepositorio,
                         MetodoPagoRepositorio metodoPagoRepositorio,
                         RepositorioFactory repositorioFactory,
                         IPasarelaPagos pasarelaPagos,
                         TarifaConfig tarifaConfig,
                         IConsultaReportesAbiertos consultaReportesAbiertos,
                         ServicioNotificaciones servicioNotificaciones,
                         ServicioAuditoria servicioAuditoria) {
        this(
                pagoRepositorio,
                metodoPagoRepositorio,
                repositorioFactory,
                pasarelaPagos,
                tarifaConfig,
                consultaReportesAbiertos,
                servicioNotificaciones,
                servicioAuditoria,
                new PropiedadesApp()
        );
    }

    @Autowired
    public ServicioPagos(PagoRepositorio pagoRepositorio,
                         MetodoPagoRepositorio metodoPagoRepositorio,
                         RepositorioFactory repositorioFactory,
                         IPasarelaPagos pasarelaPagos,
                         TarifaConfig tarifaConfig,
                         IConsultaReportesAbiertos consultaReportesAbiertos,
                         ServicioNotificaciones servicioNotificaciones,
                         ServicioAuditoria servicioAuditoria,
                         PropiedadesApp propiedadesApp) {
        this.pagoRepositorio = pagoRepositorio;
        this.metodoPagoRepositorio = metodoPagoRepositorio;
        this.solicitudRepositorio = repositorioFactory.crearSolicitudRepositorio();
        this.pasarelaPagos = pasarelaPagos;
        this.tarifaConfig = tarifaConfig;
        this.consultaReportesAbiertos = consultaReportesAbiertos;
        this.servicioNotificaciones = servicioNotificaciones;
        this.servicioAuditoria = servicioAuditoria;
        this.propiedadesApp = propiedadesApp;
    }

    @Transactional(readOnly = true)
    public ResumenPagoDTO obtenerResumenPago(Long idSolicitud, Long idUsuario) {
        Solicitud solicitud = buscarSolicitudDetalle(idSolicitud);
        if (!solicitud.esParticipante(idUsuario)) {
            throw new ExcepcionNoAutorizado("No tienes permisos para consultar el pago de esta solicitud.");
        }
        Optional<Pago> pagoOpt = buscarPagoActual(idSolicitud);
        boolean tieneReporteAbierto = consultaReportesAbiertos.existeReporteAbierto(idSolicitud);

        BigDecimal montoServicio = solicitud.getPrecioFinal();
        BigDecimal tasaGarantia = tarifaConfig.getTasaGarantiaFija();
        BigDecimal total = montoServicio != null ? montoServicio.add(tasaGarantia) : null;
        BigDecimal tasaCambio = tarifaConfig.getTasaCambioCopUsd();
        BigDecimal montoUsd = total != null ? tarifaConfig.convertirCopAUsd(total) : null;

        boolean yaTienePagoConfirmado = pagoOpt
                .map(p -> ESTADOS_PAGO_CONFIRMADO.contains(p.getEstadoPago()))
                .orElse(false);

        boolean puedePagar = solicitud.getEstadoSolicitud() == EstadoSolicitud.FINALIZADA
                && solicitud.isPrecioFinalConfirmado()
                && !yaTienePagoConfirmado;

        boolean puedeLiberarCustodia = solicitud.puedeLiberarCustodia(tieneReporteAbierto)
                && pagoOpt.map(p -> p.getEstadoPago() == EstadoPago.RETENIDO).orElse(false);

        return new ResumenPagoDTO(
                solicitud.getIdSolicitud(),
                solicitud.getEstadoSolicitud(),
                pasarelaPagos.obtenerTipoPasarela(),
                pasarelaPagos.requiereAprobacionExterna(),
                montoServicio,
                tasaGarantia,
                total,
                montoUsd,
                tasaCambio,
                puedePagar,
                puedeLiberarCustodia,
                tieneReporteAbierto,
                pagoOpt.map(p -> mapearDTO(p, false)).orElse(null)
        );
    }

    @Transactional
    public PagoDTO procesarPago(Long idSolicitud, Long idCliente, Long idMetodoPago) {
        Solicitud solicitud = buscarSolicitudDetalle(idSolicitud);
        verificarDuenoCliente(solicitud, idCliente);

        Optional<Pago> pagoExistente = pagoRepositorio.buscarPagoActivoPorSolicitud(idSolicitud, ESTADOS_PAGO_CONFIRMADO);
        if (pagoExistente.isPresent()) {
            return mapearDTO(pagoExistente.get(), true);
        }

        if (pasarelaPagos.requiereAprobacionExterna()) {
            throw new ExcepcionNegocio("La pasarela activa requiere aprobación externa (usa /pagar/iniciar y /pagar/confirmar).");
        }

        if (solicitud.getEstadoSolicitud() != EstadoSolicitud.FINALIZADA || !solicitud.isPrecioFinalConfirmado()) {
            throw new ExcepcionNegocio("Solo se pueden pagar solicitudes FINALIZADAS con precio final confirmado.");
        }

        MetodoPago metodo = metodoPagoRepositorio
                .findByIdMetodoPagoAndClienteIdUsuarioAndActivoTrue(idMetodoPago, idCliente)
                .orElseThrow(() -> new ExcepcionRecursoNoEncontrado("Método de pago no encontrado o inactivo."));
        if (metodo.estaVencido(YearMonth.now())) {
            throw new ExcepcionNegocio("La tarjeta seleccionada está vencida. Usa otro método de pago.");
        }

        BigDecimal tasaGarantia = tarifaConfig.getTasaGarantiaFija();
        String referenciaIdempotencia = PREFIJO_REFERENCIA_IDEMPOTENCIA + idSolicitud;

        Pago pago = new Pago(solicitud, metodo, solicitud.getPrecioFinal(), tasaGarantia, referenciaIdempotencia);
        pago.ejecutarCobro(pasarelaPagos);
        Pago guardado = pagoRepositorio.saveAndFlush(pago);

        if (guardado.getEstadoPago() == EstadoPago.RETENIDO) {
            finalizarPagoExitosoEnCustodia(solicitud, guardado);
        } else {
            notificarPagoRechazado(solicitud, guardado);
        }

        return mapearDTO(guardado, false);
    }

    @Transactional
    public RespuestaInicioPagoDTO iniciarPagoExterno(Long idSolicitud, Long idCliente, String appRedirectUrl) {
        if (!(pasarelaPagos instanceof IPasarelaPagoRedireccion pasarelaRedireccion)) {
            throw new ExcepcionNegocio("La pasarela configurada (" + pasarelaPagos.obtenerTipoPasarela()
                    + ") no utiliza flujo de redirección externa.");
        }

        Solicitud solicitud = buscarSolicitudDetalle(idSolicitud);
        verificarDuenoCliente(solicitud, idCliente);

        Optional<Pago> confirmado = pagoRepositorio.buscarPagoActivoPorSolicitud(idSolicitud, ESTADOS_PAGO_CONFIRMADO);
        if (confirmado.isPresent()) {
            throw new ExcepcionConflicto("Esta solicitud ya cuenta con un pago aprobado.");
        }

        Optional<Pago> pendienteOpt = pagoRepositorio.buscarPendienteAprobacionPorSolicitud(idSolicitud);
        if (pendienteOpt.isPresent()) {
            Pago pendiente = pendienteOpt.get();
            return mapearInicioDTO(pendiente, true);
        }

        if (solicitud.getEstadoSolicitud() != EstadoSolicitud.FINALIZADA || !solicitud.isPrecioFinalConfirmado()) {
            throw new ExcepcionNegocio("Solo se pueden pagar solicitudes FINALIZADAS con precio final confirmado.");
        }

        MetodoPago metodoPayPal = metodoPagoRepositorio
                .findFirstByClienteIdUsuarioAndTipoAndActivoTrue(idCliente, TipoMetodoPago.PAYPAL)
                .orElseGet(() -> metodoPagoRepositorio.save(
                        MetodoPago.crearMetodoPayPal(solicitud.getCliente(), solicitud.getCliente().getCorreo())
                ));

        BigDecimal tasaGarantia = tarifaConfig.getTasaGarantiaFija();
        BigDecimal totalCop = solicitud.getPrecioFinal().add(tasaGarantia);
        BigDecimal tasaCambioUsd = tarifaConfig.getTasaCambioCopUsd();
        BigDecimal montoUsd = tarifaConfig.convertirCopAUsd(totalCop);

        String referenciaBase = PREFIJO_REFERENCIA_IDEMPOTENCIA + idSolicitud;
        String urlRetorno = construirUrlCallback(propiedadesApp.getPagos().getPaypal().getReturnUrl(), appRedirectUrl);
        String urlCancelacion = construirUrlCallback(propiedadesApp.getPagos().getPaypal().getCancelUrl(), appRedirectUrl);
        String descripcion = "Servicio " + solicitud.getServicio().getCategoriaOficio().getNombre()
                + " - Solicitud #" + idSolicitud;

        Pago pago = new Pago(solicitud, metodoPayPal, solicitud.getPrecioFinal(), tasaGarantia, referenciaBase);
        pago.configurarConversionUsd(pasarelaRedireccion.obtenerTipoPasarela(), montoUsd, tasaCambioUsd);
        pago.iniciarAprobacionExterna(pasarelaRedireccion, descripcion, urlRetorno, urlCancelacion);

        Pago guardado = pagoRepositorio.saveAndFlush(pago);
        return mapearInicioDTO(guardado, false);
    }

    @Transactional
    public PagoDTO confirmarPagoExterno(Long idSolicitud, Long idCliente, String idOrdenExterna) {
        if (!(pasarelaPagos instanceof IPasarelaPagoRedireccion pasarelaRedireccion)) {
            throw new ExcepcionNegocio("La pasarela configurada no admite confirmación externa.");
        }

        Solicitud solicitud = buscarSolicitudDetalle(idSolicitud);
        verificarDuenoCliente(solicitud, idCliente);

        // Idempotencia ante confirmaciones duplicadas del retorno móvil o navegador
        Optional<Pago> confirmado = pagoRepositorio.buscarPagoActivoPorSolicitud(idSolicitud, ESTADOS_PAGO_CONFIRMADO);
        if (confirmado.isPresent()) {
            return mapearDTO(confirmado.get(), true);
        }

        Pago pendiente = pagoRepositorio.buscarPendienteAprobacionPorSolicitud(idSolicitud)
                .orElseThrow(() -> new ExcepcionRecursoNoEncontrado(
                        "No hay una orden de PayPal pendiente de confirmación para esta solicitud."));

        if (idOrdenExterna != null && !idOrdenExterna.isBlank()
                && !idOrdenExterna.equals(pendiente.getReferenciaOrdenExterna())) {
            throw new ExcepcionNegocio("La orden de PayPal indicada no coincide con la orden pendiente de la solicitud.");
        }

        pendiente.confirmarAutorizacionExterna(pasarelaRedireccion);
        Pago guardado = pagoRepositorio.saveAndFlush(pendiente);

        if (guardado.getEstadoPago() == EstadoPago.RETENIDO) {
            finalizarPagoExitosoEnCustodia(solicitud, guardado);
        } else {
            notificarPagoRechazado(solicitud, guardado);
        }

        return mapearDTO(guardado, false);
    }

    @Transactional
    public PagoDTO cancelarPagoPendiente(Long idSolicitud, Long idCliente) {
        Solicitud solicitud = buscarSolicitudDetalle(idSolicitud);
        verificarDuenoCliente(solicitud, idCliente);

        Pago pendiente = pagoRepositorio.buscarPendienteAprobacionPorSolicitud(idSolicitud)
                .orElseThrow(() -> new ExcepcionRecursoNoEncontrado(
                        "No existe una orden pendiente por cancelar en esta solicitud."));

        pendiente.marcarCancelado(null);
        Pago guardado = pagoRepositorio.save(pendiente);
        return mapearDTO(guardado, false);
    }

    @Transactional(readOnly = true)
    public PagoDTO obtenerPagoDeSolicitud(Long idSolicitud, Long idUsuario) {
        Solicitud solicitud = buscarSolicitudDetalle(idSolicitud);
        if (!solicitud.esParticipante(idUsuario)) {
            throw new ExcepcionNoAutorizado("No tienes acceso al comprobante de esta solicitud.");
        }
        Pago pago = buscarPagoActual(idSolicitud)
                .orElseThrow(() -> new ExcepcionRecursoNoEncontrado("Esta solicitud aún no registra pagos."));
        return mapearDTO(pago, false);
    }

    @Transactional
    public PagoDTO liberarCustodiaPorCliente(Long idSolicitud, Long idCliente) {
        Solicitud solicitud = buscarSolicitudDetalle(idSolicitud);
        verificarDuenoCliente(solicitud, idCliente);

        boolean hayReporteAbierto = consultaReportesAbiertos.existeReporteAbierto(idSolicitud);
        if (!solicitud.puedeLiberarCustodia(hayReporteAbierto)) {
            String motivo = hayReporteAbierto
                    ? "No es posible liberar los fondos mientras exista un reporte o disputa abierta sobre la solicitud."
                    : "No es posible liberar la custodia en el estado actual (" + solicitud.getEstadoSolicitud() + ").";
            throw new ExcepcionNegocio(motivo);
        }

        Pago pago = pagoRepositorio.buscarPagoActivoPorSolicitud(idSolicitud, ESTADOS_PAGO_CONFIRMADO)
                .orElseThrow(() -> new ExcepcionRecursoNoEncontrado("No existe un pago en custodia para esta solicitud."));

        pago.liberarCustodia(pasarelaPagos);
        Pago guardado = pagoRepositorio.save(pago);

        servicioNotificaciones.notificar(
                solicitud.getProfesional().getIdUsuario(),
                "Fondos liberados",
                solicitud.getCliente().getNombre() + " confirmó el servicio recibido a satisfacción. Se liberaron $"
                        + guardado.getMontoServicio() + " COP a tu favor.",
                "PAGO_LIBERADO"
        );

        return mapearDTO(guardado, false);
    }

    @Transactional
    public PagoDTO reembolsarPorModeracion(Long idSolicitud, Long idAdmin, String motivo) {
        Pago pago = pagoRepositorio.buscarPagoActivoPorSolicitud(idSolicitud, ESTADOS_PAGO_CONFIRMADO)
                .orElseThrow(() -> new ExcepcionNegocio("No existe un pago activo asociado a la solicitud para reembolsar."));

        pago.reembolsar(pasarelaPagos, motivo);
        Pago guardado = pagoRepositorio.save(pago);
        Solicitud solicitud = guardado.getSolicitud();

        servicioAuditoria.registrar(
                idAdmin,
                "REEMBOLSAR_CUSTODIA",
                "PAGO",
                guardado.getIdPago(),
                "Reembolso aprobado sobre solicitud #" + idSolicitud + ". Justificación: " + motivo
        );

        servicioNotificaciones.notificar(
                solicitud.getCliente().getIdUsuario(),
                "Reembolso aprobado",
                "La administración aprobó el reembolso de $" + guardado.getCantidad() + " COP de la solicitud #" + idSolicitud + ".",
                "PAGO_REEMBOLSADO"
        );
        servicioNotificaciones.notificar(
                solicitud.getProfesional().getIdUsuario(),
                "Pago reembolsado por moderación",
                "Los fondos retenidos de la solicitud #" + idSolicitud + " fueron reembolsados tras resolución de reporte.",
                "PAGO_REEMBOLSADO"
        );

        return mapearDTO(guardado, false);
    }

    @Transactional
    public PagoDTO liberarPorModeracion(Long idSolicitud, Long idAdmin, String motivo) {
        Pago pago = pagoRepositorio.buscarPagoActivoPorSolicitud(idSolicitud, ESTADOS_PAGO_CONFIRMADO)
                .orElseThrow(() -> new ExcepcionNegocio("No existe un pago activo asociado a la solicitud para liberar."));

        pago.liberarCustodia(pasarelaPagos);
        Pago guardado = pagoRepositorio.save(pago);
        Solicitud solicitud = guardado.getSolicitud();

        servicioAuditoria.registrar(
                idAdmin,
                "LIBERAR_CUSTODIA_MODERACION",
                "PAGO",
                guardado.getIdPago(),
                "Liberación aprobada sobre solicitud #" + idSolicitud + ". Justificación: " + motivo
        );

        servicioNotificaciones.notificar(
                solicitud.getProfesional().getIdUsuario(),
                "Fondos liberados por administración",
                "La administración autorizó liberar los fondos de la solicitud #" + idSolicitud + ".",
                "PAGO_LIBERADO"
        );
        servicioNotificaciones.notificar(
                solicitud.getCliente().getIdUsuario(),
                "Custodia resuelta",
                "La administración cerró el caso y autorizó la liberación de los fondos de la solicitud #" + idSolicitud + ".",
                "PAGO_LIBERADO"
        );

        return mapearDTO(guardado, false);
    }

    private void finalizarPagoExitosoEnCustodia(Solicitud solicitud, Pago pago) {
        solicitud.marcarPagada();
        solicitudRepositorio.guardar(solicitud);

        String detalleUsd = pago.getMontoUsd() != null ? " ($" + pago.getMontoUsd() + " USD)" : "";
        servicioNotificaciones.notificar(
                solicitud.getCliente().getIdUsuario(),
                "Pago retenido en custodia",
                "Tu pago por $" + pago.getCantidad() + " COP" + detalleUsd
                        + " (servicio $" + pago.getMontoServicio()
                        + " + garantía $" + pago.getTasaGarantia() + ") quedó protegido en custodia.",
                "PAGO_RETENIDO"
        );
        servicioNotificaciones.notificar(
                solicitud.getProfesional().getIdUsuario(),
                "Pago confirmado en custodia",
                solicitud.getCliente().getNombre() + " realizó el pago de $" + pago.getCantidad()
                        + " COP" + detalleUsd + ". El dinero se liberará cuando el cliente confirme satisfacción.",
                "PAGO_RETENIDO"
        );
    }

    private void notificarPagoRechazado(Solicitud solicitud, Pago pago) {
        servicioNotificaciones.notificar(
                solicitud.getCliente().getIdUsuario(),
                "Pago rechazado",
                "Tu intento de pago fue rechazado: " + pago.getMotivoRechazo()
                        + " Puedes reintentar con otro método de pago.",
                "PAGO_RECHAZADO"
        );
    }

    private String construirUrlCallback(String baseCallbackUrl, String appRedirectUrl) {
        if (appRedirectUrl == null || appRedirectUrl.isBlank()) {
            return baseCallbackUrl;
        }
        String limpio = appRedirectUrl.trim();
        if (!PATRON_APP_REDIRECT.matcher(limpio).matches()) {
            return baseCallbackUrl;
        }
        String separador = baseCallbackUrl.contains("?") ? "&" : "?";
        return baseCallbackUrl + separador + "appRedirect=" + URLEncoder.encode(limpio, StandardCharsets.UTF_8);
    }

    private Optional<Pago> buscarPagoActual(Long idSolicitud) {
        Optional<Pago> activo = pagoRepositorio.buscarPagoActivoPorSolicitud(idSolicitud, ESTADOS_PAGO_CONFIRMADO);
        if (activo.isPresent()) {
            return activo;
        }
        Optional<Pago> pendiente = pagoRepositorio.buscarPendienteAprobacionPorSolicitud(idSolicitud);
        if (pendiente.isPresent()) {
            return pendiente;
        }
        List<Pago> historial = pagoRepositorio.listarHistorialPorSolicitud(idSolicitud);
        return historial.isEmpty() ? Optional.empty() : Optional.of(historial.get(0));
    }

    private Solicitud buscarSolicitudDetalle(Long idSolicitud) {
        return solicitudRepositorio.buscarDetallePorId(idSolicitud)
                .orElseThrow(() -> new ExcepcionRecursoNoEncontrado("Solicitud no encontrada."));
    }

    private void verificarDuenoCliente(Solicitud solicitud, Long idCliente) {
        if (!solicitud.getCliente().getIdUsuario().equals(idCliente)) {
            throw new ExcepcionNoAutorizado("Solo el cliente titular puede ejecutar pagos sobre esta solicitud.");
        }
    }

    private RespuestaInicioPagoDTO mapearInicioDTO(Pago p, boolean idempotente) {
        return new RespuestaInicioPagoDTO(
                p.getIdPago(),
                p.getSolicitud().getIdSolicitud(),
                p.getEstadoPago(),
                p.getReferenciaOrdenExterna(),
                p.getUrlAprobacion(),
                p.getMontoServicio(),
                p.getTasaGarantia(),
                p.getCantidad(),
                p.getMontoUsd(),
                p.getTasaCambioUsd(),
                idempotente
        );
    }

    private PagoDTO mapearDTO(Pago p, boolean idempotente) {
        return new PagoDTO(
                p.getIdPago(),
                p.getSolicitud().getIdSolicitud(),
                p.getEstadoPago(),
                p.getPasarela(),
                p.getMontoServicio(),
                p.getTasaGarantia(),
                p.getCantidad(),
                p.getMontoUsd(),
                p.getTasaCambioUsd(),
                p.getReferenciaIdempotencia(),
                p.getReferenciaPasarela(),
                p.getReferenciaOrdenExterna(),
                p.getUrlAprobacion(),
                p.getAutorizacionExpiraEn(),
                p.getMotivoRechazo(),
                idempotente,
                p.getFechaPago(),
                p.generarComprobante()
        );
    }
}
