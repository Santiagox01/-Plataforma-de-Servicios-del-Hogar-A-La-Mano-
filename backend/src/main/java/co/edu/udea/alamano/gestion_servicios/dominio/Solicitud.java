package co.edu.udea.alamano.gestion_servicios.dominio;

import co.edu.udea.alamano.autenticacion_seguridad.dominio.Cliente;
import co.edu.udea.alamano.autenticacion_seguridad.dominio.Profesional;
import co.edu.udea.alamano.comun.excepcion.ExcepcionConflicto;
import co.edu.udea.alamano.comun.excepcion.ExcepcionNegocio;
import co.edu.udea.alamano.comun.validacion.ValidadorDatosComunes;
import co.edu.udea.alamano.cotizacion_tarificacion.dominio.Servicio;
import co.edu.udea.alamano.cotizacion_tarificacion.dominio.TarificadorContext;
import co.edu.udea.alamano.gestion_servicios.dominio.estado.FabricaEstadoSolicitud;
import co.edu.udea.alamano.gestion_servicios.dominio.estado.IEstadoSolicitudState;
import jakarta.persistence.CascadeType;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "solicitudes")
public class Solicitud {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_solicitud")
    private Long idSolicitud;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_cliente", nullable = false)
    private Cliente cliente;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_profesional", nullable = false)
    private Profesional profesional;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_servicio", nullable = false)
    private Servicio servicio;

    @Column(name = "descripcion", nullable = false, length = 1000)
    private String descripcion;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "solicitud_fotos", joinColumns = @JoinColumn(name = "id_solicitud"))
    @Column(name = "ruta_archivo", nullable = false, length = 500)
    private List<String> fotos = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_solicitud", nullable = false, length = 30)
    private EstadoSolicitud estadoSolicitud;

    @Column(name = "motivo_rechazo", length = 500)
    private String motivoRechazo;

    @Column(name = "es_urgente", nullable = false)
    private boolean esUrgente;

    @Column(name = "precio_estimado", nullable = false, precision = 12, scale = 2)
    private BigDecimal precioEstimado;

    @Column(name = "precio_final", precision = 12, scale = 2)
    private BigDecimal precioFinal;

    @Column(name = "precio_final_confirmado", nullable = false)
    private boolean precioFinalConfirmado = false;

    @Column(name = "fecha_solicitud", nullable = false)
    private OffsetDateTime fechaSolicitud;

    @Column(name = "fecha_finalizacion")
    private OffsetDateTime fechaFinalizacion;

    @Version
    @Column(name = "version", nullable = false)
    private Long version = 0L;

    @OneToOne(mappedBy = "solicitud", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private Chat chat;

    @OneToOne(mappedBy = "solicitud", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private Resena resena;

    protected Solicitud() {
    }

    public Solicitud(Cliente cliente,
                     Profesional profesional,
                     Servicio servicio,
                     String descripcion,
                     List<String> fotos,
                     boolean esUrgente,
                     BigDecimal precioEstimado) {
        if (descripcion == null || descripcion.isBlank()) {
            throw new ExcepcionNegocio("La descripción del problema es obligatoria.");
        }
        if (fotos != null && fotos.size() > ValidadorDatosComunes.MAX_FOTOS_SOLICITUD) {
            throw new ExcepcionNegocio("Solo se permiten hasta " + ValidadorDatosComunes.MAX_FOTOS_SOLICITUD + " fotografías por solicitud.");
        }
        this.cliente = cliente;
        this.profesional = profesional;
        this.servicio = servicio;
        this.descripcion = descripcion.trim();
        if (fotos != null) {
            this.fotos.addAll(fotos);
        }
        this.esUrgente = esUrgente;
        this.precioEstimado = precioEstimado;
        this.estadoSolicitud = EstadoSolicitud.PENDIENTE;
        this.fechaSolicitud = OffsetDateTime.now();
        this.precioFinalConfirmado = false;
    }

    public BigDecimal calcularPrecioEstimado(TarificadorContext tarificadorContext) {
        BigDecimal calculado = servicio.calcularPrecioEstimado(this.esUrgente, tarificadorContext).precioEstimado();
        this.precioEstimado = calculado;
        return calculado;
    }

    public void aceptar() {
        obtenerEstadoActual().aceptar(this);
    }

    public Chat habilitarChat() {
        if (!chatHabilitado()) {
            throw new ExcepcionNegocio("El chat solo está habilitado cuando la solicitud fue aceptada.");
        }
        if (this.chat == null) {
            this.chat = new Chat(this);
        }
        return this.chat;
    }

    public void rechazar(String motivo) {
        obtenerEstadoActual().rechazar(this, motivo);
    }

    public void marcarEnCamino() {
        obtenerEstadoActual().marcarEnCamino(this);
    }

    public void iniciarProgreso() {
        obtenerEstadoActual().iniciarProgreso(this);
    }

    public void registrarPrecioFinal(BigDecimal precioFinal) {
        obtenerEstadoActual().registrarPrecioFinal(this, precioFinal);
    }

    public void confirmarPrecioFinal() {
        obtenerEstadoActual().confirmarPrecioFinal(this);
    }

    public void rechazarPrecioFinal() {
        obtenerEstadoActual().rechazarPrecioFinal(this);
    }

    public void marcarPagada() {
        obtenerEstadoActual().marcarPagada(this);
    }

    public Resena resenar(int estrellas, String comentario) {
        if (!puedeResenarse()) {
            throw new ExcepcionNegocio("Solo se pueden calificar solicitudes finalizadas.");
        }
        if (this.resena != null) {
            throw new ExcepcionConflicto("Esta solicitud ya fue calificada.");
        }
        this.resena = new Resena(this, this.cliente, this.profesional, estrellas, comentario);
        return this.resena;
    }

    public boolean chatHabilitado() {
        return obtenerEstadoActual().chatHabilitado();
    }

    public boolean puedeResenarse() {
        return obtenerEstadoActual().puedeResenarse();
    }

    public boolean puedeLiberarCustodia() {
        return obtenerEstadoActual().puedeLiberarCustodia();
    }

    public boolean puedeLiberarCustodia(boolean existenReportesAbiertos) {
        return puedeLiberarCustodia() && !existenReportesAbiertos;
    }

    public boolean esParticipante(Long idUsuario) {
        if (idUsuario == null) {
            return false;
        }
        return cliente.getIdUsuario().equals(idUsuario) || profesional.getIdUsuario().equals(idUsuario);
    }

    public void cambiarEstadoInterno(EstadoSolicitud nuevoEstado) {
        this.estadoSolicitud = nuevoEstado;
    }

    public void asignarMotivoRechazo(String motivo) {
        this.motivoRechazo = (motivo == null || motivo.isBlank()) ? null : motivo.trim();
    }

    public void asignarPrecioFinalInmutable(BigDecimal precio) {
        if (precio == null || precio.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ExcepcionNegocio("El precio final debe ser mayor a cero.");
        }
        if (this.precioFinal != null) {
            throw new ExcepcionNegocio("El precio final ya fue registrado y es inmutable.");
        }
        this.precioFinal = precio.setScale(2, RoundingMode.HALF_UP);
    }

    public void confirmarYFnalizarInterno() {
        if (this.precioFinal == null) {
            throw new ExcepcionNegocio("El profesional aún no ha registrado el precio final.");
        }
        this.precioFinalConfirmado = true;
        this.fechaFinalizacion = OffsetDateTime.now();
        this.estadoSolicitud = EstadoSolicitud.FINALIZADA;
    }

    public void rechazarPrecioYDisputarInterno() {
        if (this.precioFinal == null) {
            throw new ExcepcionNegocio("No hay precio final registrado para rechazar.");
        }
        this.precioFinalConfirmado = false;
        this.estadoSolicitud = EstadoSolicitud.EN_DISPUTA;
    }

    private IEstadoSolicitudState obtenerEstadoActual() {
        return FabricaEstadoSolicitud.obtener(this.estadoSolicitud);
    }

    public Long getIdSolicitud() {
        return idSolicitud;
    }

    public Cliente getCliente() {
        return cliente;
    }

    public Profesional getProfesional() {
        return profesional;
    }

    public Servicio getServicio() {
        return servicio;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public List<String> getFotos() {
        return fotos;
    }

    public EstadoSolicitud getEstadoSolicitud() {
        return estadoSolicitud;
    }

    public String getMotivoRechazo() {
        return motivoRechazo;
    }

    public boolean isEsUrgente() {
        return esUrgente;
    }

    public BigDecimal getPrecioEstimado() {
        return precioEstimado;
    }

    public BigDecimal getPrecioFinal() {
        return precioFinal;
    }

    public boolean isPrecioFinalConfirmado() {
        return precioFinalConfirmado;
    }

    public OffsetDateTime getFechaSolicitud() {
        return fechaSolicitud;
    }

    public OffsetDateTime getFechaFinalizacion() {
        return fechaFinalizacion;
    }

    public Long getVersion() {
        return version;
    }

    public Chat getChat() {
        return chat;
    }

    public Resena getResena() {
        return resena;
    }
}
