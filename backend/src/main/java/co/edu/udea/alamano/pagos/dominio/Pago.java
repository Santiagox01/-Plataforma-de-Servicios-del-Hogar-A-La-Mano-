package co.edu.udea.alamano.pagos.dominio;

import co.edu.udea.alamano.comun.excepcion.ExcepcionNegocio;
import co.edu.udea.alamano.gestion_servicios.dominio.Solicitud;
import co.edu.udea.alamano.pagos.dto.ComprobantePagoDTO;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.util.Locale;
import java.util.UUID;

/**
 * Entidad Pago con custodia (escrow); colabora unicamente con las interfaces IPasarelaPagos / IPasarelaPagoRedireccion.
 */
@Entity
@Table(name = "pagos")
public class Pago {

    private static final String MONEDA_EXTERNA_USD = "USD";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_pago")
    private Long idPago;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_solicitud", nullable = false)
    private Solicitud solicitud;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_metodo_pago", nullable = false)
    private MetodoPago metodoPago;

    @Column(name = "monto_servicio", nullable = false, precision = 12, scale = 2)
    private BigDecimal montoServicio;

    @Column(name = "tasa_garantia", nullable = false, precision = 12, scale = 2)
    private BigDecimal tasaGarantia;

    @Column(name = "cantidad", nullable = false, precision = 12, scale = 2)
    private BigDecimal cantidad;

    @Column(name = "pasarela", nullable = false, length = 30)
    private String pasarela = "simulada";

    @Column(name = "monto_usd", precision = 12, scale = 2)
    private BigDecimal montoUsd;

    @Column(name = "tasa_cambio_usd", precision = 12, scale = 4)
    private BigDecimal tasaCambioUsd;

    @Column(name = "referencia_idempotencia", nullable = false, unique = true, length = 120)
    private String referenciaIdempotencia;

    @Column(name = "referencia_pasarela", length = 120)
    private String referenciaPasarela;

    @Column(name = "referencia_orden_externa", length = 120)
    private String referenciaOrdenExterna;

    @Column(name = "url_aprobacion", length = 1000)
    private String urlAprobacion;

    @Column(name = "referencia_captura", length = 120)
    private String referenciaCaptura;

    @Column(name = "autorizacion_expira_en")
    private OffsetDateTime autorizacionExpiraEn;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_pago", nullable = false, length = 20)
    private EstadoPago estadoPago;

    @Column(name = "motivo_rechazo", length = 500)
    private String motivoRechazo;

    @Column(name = "fecha_pago", nullable = false)
    private OffsetDateTime fechaPago;

    @Column(name = "fecha_actualizacion", nullable = false)
    private OffsetDateTime fechaActualizacion;

    @Version
    @Column(name = "version", nullable = false)
    private Long version = 0L;

    protected Pago() {
    }

    public Pago(Solicitud solicitud,
                MetodoPago metodoPago,
                BigDecimal montoServicio,
                BigDecimal tasaGarantia,
                String referenciaIdempotencia) {
        if (solicitud == null || metodoPago == null) {
            throw new ExcepcionNegocio("La solicitud y el método de pago son obligatorios.");
        }
        if (montoServicio == null || montoServicio.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ExcepcionNegocio("El valor del servicio debe ser mayor a cero.");
        }
        if (tasaGarantia == null || tasaGarantia.compareTo(BigDecimal.ZERO) < 0) {
            throw new ExcepcionNegocio("La tasa de garantía no puede ser negativa.");
        }
        this.solicitud = solicitud;
        this.metodoPago = metodoPago;
        this.montoServicio = montoServicio.setScale(2, RoundingMode.HALF_UP);
        this.tasaGarantia = tasaGarantia.setScale(2, RoundingMode.HALF_UP);
        this.cantidad = this.montoServicio.add(this.tasaGarantia).setScale(2, RoundingMode.HALF_UP);
        this.referenciaIdempotencia = referenciaIdempotencia;
        this.pasarela = "simulada";
        this.fechaPago = OffsetDateTime.now();
        this.fechaActualizacion = this.fechaPago;
    }

    public void configurarConversionUsd(String pasarela, BigDecimal montoUsd, BigDecimal tasaCambioUsd) {
        this.pasarela = pasarela != null ? pasarela : "simulada";
        this.montoUsd = montoUsd != null ? montoUsd.setScale(2, RoundingMode.HALF_UP) : null;
        this.tasaCambioUsd = tasaCambioUsd != null ? tasaCambioUsd.setScale(4, RoundingMode.HALF_UP) : null;
    }

    public void ejecutarCobro(IPasarelaPagos pasarelaPagos) {
        this.pasarela = pasarelaPagos.obtenerTipoPasarela();
        ResultadoTransaccionPasarela resultado = pasarelaPagos.procesarCobro(
                metodoPago.getToken(),
                cantidad,
                referenciaIdempotencia
        );
        this.fechaActualizacion = OffsetDateTime.now();
        if (resultado.aprobado()) {
            this.estadoPago = EstadoPago.RETENIDO;
            this.referenciaPasarela = resultado.referenciaPasarela();
            this.motivoRechazo = null;
        } else {
            this.estadoPago = EstadoPago.RECHAZADO;
            this.motivoRechazo = resultado.motivoRechazo();
            liberarReferenciaParaReintento("RECH");
        }
    }

    public void iniciarAprobacionExterna(IPasarelaPagoRedireccion pasarelaRedireccion,
                                         String descripcion,
                                         String urlRetornoPersonalizada,
                                         String urlCancelacionPersonalizada) {
        if (this.montoUsd == null || this.montoUsd.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ExcepcionNegocio("El monto convertido a USD es obligatorio para iniciar el pago externo.");
        }
        this.pasarela = pasarelaRedireccion.obtenerTipoPasarela();
        ResultadoInicioPago inicio = pasarelaRedireccion.iniciarPago(
                this.montoUsd,
                MONEDA_EXTERNA_USD,
                this.referenciaIdempotencia,
                descripcion,
                urlRetornoPersonalizada,
                urlCancelacionPersonalizada
        );
        if (!inicio.creado()) {
            throw new ExcepcionNegocio(inicio.motivoError() != null
                    ? inicio.motivoError()
                    : "No fue posible iniciar el pago en la pasarela externa.");
        }
        this.referenciaOrdenExterna = inicio.idOrdenExterna();
        this.urlAprobacion = inicio.urlAprobacion();
        this.estadoPago = EstadoPago.PENDIENTE_APROBACION;
        this.motivoRechazo = null;
        this.fechaActualizacion = OffsetDateTime.now();
    }

    public void confirmarAutorizacionExterna(IPasarelaPagoRedireccion pasarelaRedireccion) {
        if (this.estadoPago != EstadoPago.PENDIENTE_APROBACION) {
            throw new ExcepcionNegocio("El pago no se encuentra pendiente de aprobación externa.");
        }
        ResultadoAutorizacionPago resultado = pasarelaRedireccion.confirmarYAutorizarPago(
                this.referenciaOrdenExterna,
                this.referenciaIdempotencia
        );
        this.fechaActualizacion = OffsetDateTime.now();
        if (resultado.aprobado()) {
            this.estadoPago = EstadoPago.RETENIDO;
            this.referenciaPasarela = resultado.idAutorizacion();
            this.autorizacionExpiraEn = resultado.expiraEn();
            this.motivoRechazo = null;
        } else {
            this.estadoPago = EstadoPago.RECHAZADO;
            this.motivoRechazo = resultado.motivoRechazo();
            liberarReferenciaParaReintento("RECH");
        }
    }

    public void marcarCancelado(String motivo) {
        if (this.estadoPago != EstadoPago.PENDIENTE_APROBACION) {
            throw new ExcepcionNegocio("Solo se pueden cancelar intentos de pago pendientes de aprobación.");
        }
        this.estadoPago = EstadoPago.CANCELADO;
        this.motivoRechazo = (motivo == null || motivo.isBlank())
                ? "Pago cancelado por el cliente antes de aprobar en PayPal."
                : motivo.trim();
        this.fechaActualizacion = OffsetDateTime.now();
        liberarReferenciaParaReintento("CANC");
    }

    public void liberarCustodia(IPasarelaPagos pasarelaPagos) {
        if (this.estadoPago != EstadoPago.RETENIDO) {
            throw new ExcepcionNegocio("Solo se pueden liberar fondos que estén retenidos en custodia.");
        }
        if (this.autorizacionExpiraEn != null && this.autorizacionExpiraEn.isBefore(OffsetDateTime.now())) {
            throw new ExcepcionNegocio("La autorización retenida expiró en la pasarela y requiere gestión manual.");
        }
        BigDecimal montoPasarela = resolverMontoParaPasarela(pasarelaPagos);
        ResultadoTransaccionPasarela resultado = pasarelaPagos.liberarFondos(this.referenciaPasarela, montoPasarela);
        if (!resultado.aprobado()) {
            throw new ExcepcionNegocio(resultado.motivoRechazo() != null
                    ? resultado.motivoRechazo()
                    : "La pasarela no pudo liberar los fondos en custodia.");
        }
        this.referenciaCaptura = resultado.referenciaPasarela();
        this.estadoPago = EstadoPago.LIBERADO;
        this.fechaActualizacion = OffsetDateTime.now();
    }

    public void reembolsar(IPasarelaPagos pasarelaPagos, String motivo) {
        if (this.estadoPago != EstadoPago.RETENIDO && this.estadoPago != EstadoPago.LIBERADO) {
            throw new ExcepcionNegocio("Solo se pueden reembolsar o anular pagos retenidos o liberados.");
        }
        BigDecimal montoPasarela = resolverMontoParaPasarela(pasarelaPagos);
        ResultadoTransaccionPasarela resultado = (this.estadoPago == EstadoPago.RETENIDO)
                ? pasarelaPagos.reembolsarFondos(this.referenciaPasarela, montoPasarela, motivo)
                : pasarelaPagos.reembolsarCaptura(
                        this.referenciaCaptura != null ? this.referenciaCaptura : this.referenciaPasarela,
                        montoPasarela,
                        motivo);
        if (!resultado.aprobado()) {
            throw new ExcepcionNegocio(resultado.motivoRechazo() != null
                    ? resultado.motivoRechazo()
                    : "La pasarela no pudo procesar el reembolso o anulación.");
        }
        this.estadoPago = EstadoPago.REEMBOLSADO;
        this.motivoRechazo = motivo;
        this.fechaActualizacion = OffsetDateTime.now();
    }

    public ComprobantePagoDTO generarComprobante() {
        if (this.estadoPago == null
                || this.estadoPago == EstadoPago.RECHAZADO
                || this.estadoPago == EstadoPago.CANCELADO
                || this.estadoPago == EstadoPago.PENDIENTE_APROBACION) {
            return null;
        }
        return new ComprobantePagoDTO(
                construirNumeroComprobante(),
                solicitud.getIdSolicitud(),
                solicitud.getServicio().getCategoriaOficio().getNombre(),
                solicitud.getCliente().getNombre(),
                solicitud.getProfesional().getNombre(),
                metodoPago.resumenEnmascarado(),
                pasarela,
                montoServicio,
                tasaGarantia,
                cantidad,
                montoUsd,
                tasaCambioUsd,
                estadoPago,
                referenciaPasarela,
                fechaPago
        );
    }

    public String construirNumeroComprobante() {
        String idTexto = idPago != null ? String.valueOf(idPago) : "TMP";
        return "COMP-" + idTexto + "-" + referenciaIdempotencia;
    }

    private BigDecimal resolverMontoParaPasarela(IPasarelaPagos pasarelaPagos) {
        return (pasarelaPagos.requiereAprobacionExterna() && this.montoUsd != null)
                ? this.montoUsd
                : this.cantidad;
    }

    private void liberarReferenciaParaReintento(String etiqueta) {
        this.referenciaIdempotencia = this.referenciaIdempotencia
                + "-" + etiqueta + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(Locale.ROOT);
    }

    public Long getIdPago() {
        return idPago;
    }

    public Solicitud getSolicitud() {
        return solicitud;
    }

    public MetodoPago getMetodoPago() {
        return metodoPago;
    }

    public BigDecimal getMontoServicio() {
        return montoServicio;
    }

    public BigDecimal getTasaGarantia() {
        return tasaGarantia;
    }

    public BigDecimal getCantidad() {
        return cantidad;
    }

    public String getPasarela() {
        return pasarela;
    }

    public BigDecimal getMontoUsd() {
        return montoUsd;
    }

    public BigDecimal getTasaCambioUsd() {
        return tasaCambioUsd;
    }

    public String getReferenciaIdempotencia() {
        return referenciaIdempotencia;
    }

    public String getReferenciaPasarela() {
        return referenciaPasarela;
    }

    public String getReferenciaOrdenExterna() {
        return referenciaOrdenExterna;
    }

    public String getUrlAprobacion() {
        return urlAprobacion;
    }

    public String getReferenciaCaptura() {
        return referenciaCaptura;
    }

    public OffsetDateTime getAutorizacionExpiraEn() {
        return autorizacionExpiraEn;
    }

    public EstadoPago getEstadoPago() {
        return estadoPago;
    }

    public String getMotivoRechazo() {
        return motivoRechazo;
    }

    public OffsetDateTime getFechaPago() {
        return fechaPago;
    }

    public OffsetDateTime getFechaActualizacion() {
        return fechaActualizacion;
    }

    public Long getVersion() {
        return version;
    }
}
