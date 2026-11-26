package co.edu.udea.alamano.gestion_servicios.dominio;

import co.edu.udea.alamano.autenticacion_seguridad.dominio.Usuario;
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

@Entity
@Table(name = "mensajes")
public class Mensaje {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_mensaje")
    private Long idMensaje;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_chat", nullable = false)
    private Chat chat;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_remitente", nullable = false)
    private Usuario remitente;

    @Column(name = "contenido", nullable = false, length = 1000)
    private String contenido;

    @Column(name = "fecha_envio", nullable = false)
    private OffsetDateTime fechaEnvio;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_mensaje", nullable = false, length = 20)
    private EstadoMensaje estadoMensaje;

    protected Mensaje() {
    }

    public Mensaje(Chat chat, Usuario remitente, String contenido) {
        this.chat = chat;
        this.remitente = remitente;
        this.contenido = contenido;
        this.fechaEnvio = OffsetDateTime.now();
        this.estadoMensaje = EstadoMensaje.ENVIADO;
    }

    public void marcarComoLeido() {
        this.estadoMensaje = EstadoMensaje.LEIDO;
    }

    public Long getIdMensaje() {
        return idMensaje;
    }

    public Chat getChat() {
        return chat;
    }

    public Usuario getRemitente() {
        return remitente;
    }

    public String getContenido() {
        return contenido;
    }

    public OffsetDateTime getFechaEnvio() {
        return fechaEnvio;
    }

    public EstadoMensaje getEstadoMensaje() {
        return estadoMensaje;
    }
}
