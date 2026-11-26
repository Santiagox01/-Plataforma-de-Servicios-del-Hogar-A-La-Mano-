package co.edu.udea.alamano.autenticacion_seguridad.dominio;

import co.edu.udea.alamano.comun.excepcion.ExcepcionNegocio;
import co.edu.udea.alamano.cotizacion_tarificacion.dominio.CategoriaOficio;
import co.edu.udea.alamano.cotizacion_tarificacion.dominio.Servicio;
import jakarta.persistence.CascadeType;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "profesionales")
@PrimaryKeyJoinColumn(name = "id_usuario")
public class Profesional extends Usuario {

    @Column(name = "ubicacion", nullable = false, length = 255)
    private String ubicacion;

    @Column(name = "latitud")
    private Double latitud;

    @Column(name = "longitud")
    private Double longitud;

    @Column(name = "documento_identidad", nullable = false, length = 500)
    private String documentoIdentidad;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "profesional_certificaciones", joinColumns = @JoinColumn(name = "id_profesional"))
    @Column(name = "ruta_archivo", nullable = false, length = 500)
    private List<String> documentosCertificacion = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_verificacion", nullable = false, length = 40)
    private EstadoVerificacion estadoVerificacion;

    @Column(name = "motivo_rechazo", length = 500)
    private String motivoRechazo;

    @Column(name = "fecha_verificacion")
    private OffsetDateTime fechaVerificacion;

    @Column(name = "id_admin_verificador")
    private Long idAdminVerificador;

    @Column(name = "disponible", nullable = false)
    private boolean disponible = false;

    @OneToMany(mappedBy = "profesional", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Servicio> servicios = new ArrayList<>();

    protected Profesional() {
    }

    public Profesional(String nombre,
                       String correo,
                       String celular,
                       String contrasenaHash,
                       String ubicacion,
                       Double latitud,
                       Double longitud,
                       String documentoIdentidad,
                       List<String> documentosCertificacion) {
        super(nombre, correo, celular, contrasenaHash, RolUsuario.PROFESIONAL);
        this.ubicacion = ubicacion;
        this.latitud = latitud;
        this.longitud = longitud;
        this.documentoIdentidad = documentoIdentidad;
        if (documentosCertificacion != null) {
            this.documentosCertificacion.addAll(documentosCertificacion);
        }
        // Regla de negocio: todo profesional nace pendiente de verificacion y no disponible
        this.estadoVerificacion = EstadoVerificacion.PENDIENTE_VERIFICACION;
        this.disponible = false;
    }

    public void agregarServicio(CategoriaOficio categoria) {
        boolean yaExiste = servicios.stream()
                .anyMatch(s -> s.getCategoriaOficio().getIdCategoria().equals(categoria.getIdCategoria()));
        if (!yaExiste) {
            servicios.add(new Servicio(this, categoria));
        }
    }

    public void verificar(Administrador admin) {
        if (admin == null || admin.getIdUsuario() == null) {
            throw new ExcepcionNegocio("Se requiere un administrador válido para verificar.");
        }
        this.estadoVerificacion = EstadoVerificacion.VERIFICADO;
        this.motivoRechazo = null;
        this.fechaVerificacion = OffsetDateTime.now();
        this.idAdminVerificador = admin.getIdUsuario();
    }

    public void rechazarVerificacion(Administrador admin, String motivo) {
        if (admin == null || admin.getIdUsuario() == null) {
            throw new ExcepcionNegocio("Se requiere un administrador válido para rechazar.");
        }
        if (motivo == null || motivo.isBlank()) {
            throw new ExcepcionNegocio("Debes indicar un motivo al rechazar la verificación.");
        }
        this.estadoVerificacion = EstadoVerificacion.RECHAZADO;
        this.motivoRechazo = motivo.trim();
        this.fechaVerificacion = OffsetDateTime.now();
        this.idAdminVerificador = admin.getIdUsuario();
        this.disponible = false;
    }

    public void recargarDocumentoIdentidad(String nuevaRutaDocumento, List<String> nuevasCertificaciones) {
        if (nuevaRutaDocumento == null || nuevaRutaDocumento.isBlank()) {
            throw new ExcepcionNegocio("El documento de identidad es obligatorio.");
        }
        this.documentoIdentidad = nuevaRutaDocumento;
        if (nuevasCertificaciones != null && !nuevasCertificaciones.isEmpty()) {
            this.documentosCertificacion.clear();
            this.documentosCertificacion.addAll(nuevasCertificaciones);
        }
        this.estadoVerificacion = EstadoVerificacion.PENDIENTE_VERIFICACION;
        this.motivoRechazo = null;
        this.disponible = false;
    }

    public void establecerDisponibilidad(boolean disponible) {
        if (disponible && this.estadoVerificacion != EstadoVerificacion.VERIFICADO) {
            throw new ExcepcionNegocio("No puedes activarte como disponible hasta ser verificado por un administrador.");
        }
        if (disponible && !estaActiva()) {
            throw new ExcepcionNegocio("Tu cuenta no está activa.");
        }
        this.disponible = disponible;
    }

    @Override
    public void suspenderTemporalmente(OffsetDateTime hasta, String motivo) {
        super.suspenderTemporalmente(hasta, motivo);
        this.disponible = false;
    }

    @Override
    public void desactivarCuenta(String motivo) {
        super.desactivarCuenta(motivo);
        this.disponible = false;
    }

    public boolean esVisibleEnBusqueda() {
        return estaActiva()
                && !estaSuspendidaVigente(OffsetDateTime.now())
                && this.estadoVerificacion == EstadoVerificacion.VERIFICADO
                && this.disponible;
    }

    public String getUbicacion() {
        return ubicacion;
    }

    public Double getLatitud() {
        return latitud;
    }

    public Double getLongitud() {
        return longitud;
    }

    public String getDocumentoIdentidad() {
        return documentoIdentidad;
    }

    public List<String> getDocumentosCertificacion() {
        return documentosCertificacion;
    }

    public EstadoVerificacion getEstadoVerificacion() {
        return estadoVerificacion;
    }

    public String getMotivoRechazo() {
        return motivoRechazo;
    }

    public OffsetDateTime getFechaVerificacion() {
        return fechaVerificacion;
    }

    public Long getIdAdminVerificador() {
        return idAdminVerificador;
    }

    public boolean isDisponible() {
        return disponible;
    }

    public List<Servicio> getServicios() {
        return servicios;
    }
}
