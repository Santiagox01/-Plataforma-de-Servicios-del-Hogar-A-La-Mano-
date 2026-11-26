package co.edu.udea.alamano.comun.notificacion;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;

@Entity
@Table(name = "notificaciones")
public class Notificacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_notificacion")
    private Long idNotificacion;

    @Column(name = "id_usuario", nullable = false)
    private Long idUsuario;

    @Column(name = "titulo", nullable = false, length = 160)
    private String titulo;

    @Column(name = "mensaje", nullable = false, length = 600)
    private String mensaje;

    @Column(name = "tipo", nullable = false, length = 60)
    private String tipo;

    @Column(name = "leida", nullable = false)
    private boolean leida = false;

    @Column(name = "fecha_creacion", nullable = false)
    private OffsetDateTime fechaCreacion;

    protected Notificacion() {
    }

    public Notificacion(Long idUsuario, String titulo, String mensaje, String tipo) {
        this.idUsuario = idUsuario;
        this.titulo = titulo;
        this.mensaje = mensaje;
        this.tipo = tipo;
        this.leida = false;
        this.fechaCreacion = OffsetDateTime.now();
    }

    public void marcarLeida() {
        this.leida = true;
    }

    public Long getIdNotificacion() {
        return idNotificacion;
    }

    public Long getIdUsuario() {
        return idUsuario;
    }

    public String getTitulo() {
        return titulo;
    }

    public String getMensaje() {
        return mensaje;
    }

    public String getTipo() {
        return tipo;
    }

    public boolean isLeida() {
        return leida;
    }

    public OffsetDateTime getFechaCreacion() {
        return fechaCreacion;
    }
}
