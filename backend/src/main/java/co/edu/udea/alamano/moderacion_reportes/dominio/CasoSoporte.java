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

import java.time.OffsetDateTime;
import java.time.Year;
import java.util.Locale;
import java.util.UUID;

@Entity
@Table(name = "casos_soporte")
public class CasoSoporte {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_caso")
    private Long idCaso;

    @Column(name = "numero_caso", nullable = false, unique = true, length = 40)
    private String numeroCaso;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_usuario", nullable = false)
    private Usuario usuario;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_solicitud")
    private Solicitud solicitud;

    @Column(name = "asunto", nullable = false, length = 160)
    private String asunto;

    @Column(name = "descripcion", nullable = false, length = 1200)
    private String descripcion;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_caso", nullable = false, length = 20)
    private EstadoCasoSoporte estadoCaso;

    @Column(name = "respuesta_admin", length = 1200)
    private String respuestaAdmin;

    @Column(name = "id_admin_respondedor")
    private Long idAdminRespondedor;

    @Column(name = "fecha_creacion", nullable = false)
    private OffsetDateTime fechaCreacion;

    @Column(name = "fecha_respuesta")
    private OffsetDateTime fechaRespuesta;

    protected CasoSoporte() {
    }

    public CasoSoporte(Usuario usuario, Solicitud solicitud, String asunto, String descripcion) {
        if (usuario == null) {
            throw new ExcepcionNegocio("El usuario solicitante es obligatorio.");
        }
        if (asunto == null || asunto.isBlank() || descripcion == null || descripcion.isBlank()) {
            throw new ExcepcionNegocio("El asunto y la descripción del caso de soporte son obligatorios.");
        }
        this.numeroCaso = generarNumeroUnico();
        this.usuario = usuario;
        this.solicitud = solicitud;
        this.asunto = asunto.trim();
        this.descripcion = descripcion.trim();
        this.estadoCaso = EstadoCasoSoporte.ABIERTO;
        this.fechaCreacion = OffsetDateTime.now();
    }

    public void responder(Administrador admin, String respuesta, boolean cerrar) {
        if (this.estadoCaso == EstadoCasoSoporte.CERRADO) {
            throw new ExcepcionConflicto("Este caso de soporte ya fue cerrado.");
        }
        if (admin == null || admin.getIdUsuario() == null) {
            throw new ExcepcionNegocio("Se requiere un administrador válido para responder el caso.");
        }
        if (respuesta == null || respuesta.isBlank()) {
            throw new ExcepcionNegocio("La respuesta del administrador es obligatoria.");
        }
        this.respuestaAdmin = respuesta.trim();
        this.idAdminRespondedor = admin.getIdUsuario();
        this.fechaRespuesta = OffsetDateTime.now();
        this.estadoCaso = cerrar ? EstadoCasoSoporte.CERRADO : EstadoCasoSoporte.RESPONDIDO;
    }

    private static String generarNumeroUnico() {
        String sufijo = UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase(Locale.ROOT);
        return "SUP-" + Year.now().getValue() + "-" + sufijo;
    }

    public Long getIdCaso() {
        return idCaso;
    }

    public String getNumeroCaso() {
        return numeroCaso;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public Solicitud getSolicitud() {
        return solicitud;
    }

    public String getAsunto() {
        return asunto;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public EstadoCasoSoporte getEstadoCaso() {
        return estadoCaso;
    }

    public String getRespuestaAdmin() {
        return respuestaAdmin;
    }

    public Long getIdAdminRespondedor() {
        return idAdminRespondedor;
    }

    public OffsetDateTime getFechaCreacion() {
        return fechaCreacion;
    }

    public OffsetDateTime getFechaRespuesta() {
        return fechaRespuesta;
    }
}
