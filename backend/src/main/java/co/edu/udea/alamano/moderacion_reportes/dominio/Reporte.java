package co.edu.udea.alamano.moderacion_reportes.dominio;

import co.edu.udea.alamano.autenticacion_seguridad.dominio.Administrador;
import co.edu.udea.alamano.autenticacion_seguridad.dominio.Usuario;
import co.edu.udea.alamano.comun.excepcion.ExcepcionConflicto;
import co.edu.udea.alamano.comun.excepcion.ExcepcionNegocio;
import co.edu.udea.alamano.gestion_servicios.dominio.Solicitud;
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

import java.time.OffsetDateTime;

@Entity
@Table(name = "reportes")
public class Reporte {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_reporte")
    private Long idReporte;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_solicitud", nullable = false)
    private Solicitud solicitud;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_reportante", nullable = false)
    private Usuario reportante;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_reportado", nullable = false)
    private Usuario reportado;

    @Enumerated(EnumType.STRING)
    @Column(name = "motivo", nullable = false, length = 40)
    private MotivoReporte motivo;

    @Column(name = "descripcion", nullable = false, length = 1000)
    private String descripcion;

    @Column(name = "es_prioritario", nullable = false)
    private boolean esPrioritario;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_reporte", nullable = false, length = 20)
    private EstadoReporte estadoReporte;

    @Enumerated(EnumType.STRING)
    @Column(name = "accion_tomada", length = 30)
    private AccionModeracion accionTomada;

    @Column(name = "justificacion_resolucion", length = 1000)
    private String justificacionResolucion;

    @Column(name = "dias_suspension")
    private Integer diasSuspension;

    @Enumerated(EnumType.STRING)
    @Column(name = "decision_pago", length = 20)
    private DecisionPagoModeracion decisionPago;

    @Column(name = "id_admin_resolutor")
    private Long idAdminResolutor;

    @Column(name = "fecha_reporte", nullable = false)
    private OffsetDateTime fechaReporte;

    @Column(name = "fecha_resolucion")
    private OffsetDateTime fechaResolucion;

    @Version
    @Column(name = "version", nullable = false)
    private Long version = 0L;

    protected Reporte() {
    }

    public Reporte(Solicitud solicitud,
                   Usuario reportante,
                   Usuario reportado,
                   MotivoReporte motivo,
                   String descripcion,
                   boolean esPrioritario) {
        if (solicitud == null || reportante == null || reportado == null) {
            throw new ExcepcionNegocio("El reporte debe estar vinculado a una solicitud y a ambas partes.");
        }
        if (motivo == null) {
            throw new ExcepcionNegocio("Debes seleccionar un motivo válido para el reporte.");
        }
        if (descripcion == null || descripcion.isBlank()) {
            throw new ExcepcionNegocio("La descripción de lo ocurrido es obligatoria.");
        }
        this.solicitud = solicitud;
        this.reportante = reportante;
        this.reportado = reportado;
        this.motivo = motivo;
        this.descripcion = descripcion.trim();
        this.esPrioritario = esPrioritario;
        this.estadoReporte = EstadoReporte.PENDIENTE;
        this.fechaReporte = OffsetDateTime.now();
    }

    public void resolver(Administrador admin,
                         AccionModeracion accion,
                         String justificacion,
                         Integer diasSuspension,
                         DecisionPagoModeracion decisionPago) {
        if (this.estadoReporte != EstadoReporte.PENDIENTE) {
            throw new ExcepcionConflicto("La resolución de este reporte ya fue cerrada y es inmutable.");
        }
        if (admin == null || admin.getIdUsuario() == null) {
            throw new ExcepcionNegocio("Se requiere un administrador válido para resolver el caso.");
        }
        if (accion == null) {
            throw new ExcepcionNegocio("Debes elegir una acción de moderación.");
        }
        if (justificacion == null || justificacion.isBlank()) {
            throw new ExcepcionNegocio("La justificación de la resolución es obligatoria.");
        }
        if (accion == AccionModeracion.SUSPENSION_TEMPORAL && (diasSuspension == null || diasSuspension < 1)) {
            throw new ExcepcionNegocio("La suspensión temporal debe indicar al menos 1 día.");
        }
        this.accionTomada = accion;
        this.justificacionResolucion = justificacion.trim();
        this.diasSuspension = accion == AccionModeracion.SUSPENSION_TEMPORAL ? diasSuspension : null;
        this.decisionPago = decisionPago != null ? decisionPago : DecisionPagoModeracion.SIN_CAMBIO;
        this.idAdminResolutor = admin.getIdUsuario();
        this.fechaResolucion = OffsetDateTime.now();
        this.estadoReporte = (accion == AccionModeracion.DESCARTAR)
                ? EstadoReporte.DESCARTADO
                : EstadoReporte.RESUELTO;
    }

    public boolean estaAbierto() {
        return this.estadoReporte == EstadoReporte.PENDIENTE;
    }

    public Long getIdReporte() {
        return idReporte;
    }

    public Solicitud getSolicitud() {
        return solicitud;
    }

    public Usuario getReportante() {
        return reportante;
    }

    public Usuario getReportado() {
        return reportado;
    }

    public MotivoReporte getMotivo() {
        return motivo;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public boolean isEsPrioritario() {
        return esPrioritario;
    }

    public EstadoReporte getEstadoReporte() {
        return estadoReporte;
    }

    public AccionModeracion getAccionTomada() {
        return accionTomada;
    }

    public String getJustificacionResolucion() {
        return justificacionResolucion;
    }

    public Integer getDiasSuspension() {
        return diasSuspension;
    }

    public DecisionPagoModeracion getDecisionPago() {
        return decisionPago;
    }

    public Long getIdAdminResolutor() {
        return idAdminResolutor;
    }

    public OffsetDateTime getFechaReporte() {
        return fechaReporte;
    }

    public OffsetDateTime getFechaResolucion() {
        return fechaResolucion;
    }

    public Long getVersion() {
        return version;
    }
}
