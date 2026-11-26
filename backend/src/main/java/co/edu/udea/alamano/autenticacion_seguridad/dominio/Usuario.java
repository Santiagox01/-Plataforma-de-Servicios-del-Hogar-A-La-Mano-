package co.edu.udea.alamano.autenticacion_seguridad.dominio;

import co.edu.udea.alamano.comun.excepcion.ExcepcionNegocio;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;

@Entity
@Table(name = "usuarios")
@Inheritance(strategy = InheritanceType.JOINED)
public abstract class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_usuario")
    private Long idUsuario;

    @Column(name = "nombre", nullable = false, length = 120)
    private String nombre;

    @Column(name = "correo", nullable = false, unique = true, length = 150)
    private String correo;

    @Column(name = "celular", nullable = false, length = 20)
    private String celular;

    @Column(name = "contrasena", nullable = false, length = 255)
    private String contrasena;

    @Enumerated(EnumType.STRING)
    @Column(name = "rol", nullable = false, length = 30)
    private RolUsuario rol;

    @Column(name = "fecha_registro", nullable = false)
    private OffsetDateTime fechaRegistro;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_cuenta", nullable = false, length = 30)
    private EstadoCuenta estadoCuenta;

    @Column(name = "intentos_fallidos", nullable = false)
    private int intentosFallidos = 0;

    @Column(name = "bloqueado_hasta")
    private OffsetDateTime bloqueadoHasta;

    @Column(name = "foto_perfil", length = 500)
    private String fotoPerfil;

    @Column(name = "suspendido_hasta")
    private OffsetDateTime suspendidoHasta;

    @Column(name = "motivo_suspension", length = 500)
    private String motivoSuspension;

    protected Usuario() {
    }

    protected Usuario(String nombre, String correo, String celular, String contrasenaHash, RolUsuario rol) {
        this.nombre = nombre;
        this.correo = correo;
        this.celular = celular;
        this.contrasena = contrasenaHash;
        this.rol = rol;
        this.fechaRegistro = OffsetDateTime.now();
        this.estadoCuenta = EstadoCuenta.ACTIVA;
        this.intentosFallidos = 0;
    }

    public boolean estaBloqueado(OffsetDateTime ahora) {
        return bloqueadoHasta != null && bloqueadoHasta.isAfter(ahora);
    }

    public boolean registrarIntentoFallido(int maxIntentos, int minutosBloqueo, OffsetDateTime ahora) {
        if (bloqueadoHasta != null && !bloqueadoHasta.isAfter(ahora)) {
            this.intentosFallidos = 0;
            this.bloqueadoHasta = null;
        }
        this.intentosFallidos++;
        if (this.intentosFallidos >= maxIntentos) {
            this.bloqueadoHasta = ahora.plusMinutes(minutosBloqueo);
            return true;
        }
        return false;
    }

    public void reiniciarIntentosFallidos() {
        this.intentosFallidos = 0;
        this.bloqueadoHasta = null;
    }

    public void suspenderCuenta() {
        this.estadoCuenta = EstadoCuenta.SUSPENDIDA;
    }

    public void suspenderTemporalmente(OffsetDateTime hasta, String motivo) {
        if (hasta == null || !hasta.isAfter(OffsetDateTime.now())) {
            throw new ExcepcionNegocio("La fecha de fin de suspensión debe ser futura.");
        }
        if (motivo == null || motivo.isBlank()) {
            throw new ExcepcionNegocio("La justificación de la suspensión es obligatoria.");
        }
        this.estadoCuenta = EstadoCuenta.SUSPENDIDA;
        this.suspendidoHasta = hasta;
        this.motivoSuspension = motivo.trim();
    }

    public void desactivarCuenta() {
        desactivarCuenta(null);
    }

    public void desactivarCuenta(String motivo) {
        this.estadoCuenta = EstadoCuenta.DESACTIVADA;
        this.suspendidoHasta = null;
        this.motivoSuspension = (motivo == null || motivo.isBlank()) ? null : motivo.trim();
    }

    public boolean levantarSuspensionSiExpiro(OffsetDateTime ahora) {
        if (this.estadoCuenta == EstadoCuenta.SUSPENDIDA
                && this.suspendidoHasta != null
                && !this.suspendidoHasta.isAfter(ahora)) {
            this.estadoCuenta = EstadoCuenta.ACTIVA;
            this.suspendidoHasta = null;
            this.motivoSuspension = null;
            return true;
        }
        return false;
    }

    public boolean estaSuspendidaVigente(OffsetDateTime ahora) {
        return this.estadoCuenta == EstadoCuenta.SUSPENDIDA
                && (this.suspendidoHasta == null || this.suspendidoHasta.isAfter(ahora));
    }

    public boolean estaActiva() {
        return this.estadoCuenta == EstadoCuenta.ACTIVA;
    }

    public Long getIdUsuario() {
        return idUsuario;
    }

    public String getNombre() {
        return nombre;
    }

    public String getCorreo() {
        return correo;
    }

    public String getCelular() {
        return celular;
    }

    public String getContrasena() {
        return contrasena;
    }

    public RolUsuario getRol() {
        return rol;
    }

    public OffsetDateTime getFechaRegistro() {
        return fechaRegistro;
    }

    public EstadoCuenta getEstadoCuenta() {
        return estadoCuenta;
    }

    public int getIntentosFallidos() {
        return intentosFallidos;
    }

    public OffsetDateTime getBloqueadoHasta() {
        return bloqueadoHasta;
    }

    public String getFotoPerfil() {
        return fotoPerfil;
    }

    public void setFotoPerfil(String fotoPerfil) {
        this.fotoPerfil = fotoPerfil;
    }

    public OffsetDateTime getSuspendidoHasta() {
        return suspendidoHasta;
    }

    public String getMotivoSuspension() {
        return motivoSuspension;
    }
}
