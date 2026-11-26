package co.edu.udea.alamano.gestion_servicios.dominio;

import co.edu.udea.alamano.autenticacion_seguridad.dominio.Usuario;
import co.edu.udea.alamano.comun.excepcion.ExcepcionNegocio;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "chats")
public class Chat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_chat")
    private Long idChat;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_solicitud", nullable = false, unique = true)
    private Solicitud solicitud;

    @Column(name = "fecha_creacion", nullable = false)
    private OffsetDateTime fechaCreacion;

    @OneToMany(mappedBy = "chat", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("fechaEnvio ASC")
    private List<Mensaje> mensajes = new ArrayList<>();

    protected Chat() {
    }

    public Chat(Solicitud solicitud) {
        this.solicitud = solicitud;
        this.fechaCreacion = OffsetDateTime.now();
    }

    public Mensaje enviarMensaje(Usuario remitente, String contenido) {
        if (contenido == null || contenido.isBlank()) {
            throw new ExcepcionNegocio("El mensaje no puede estar vacío.");
        }
        String limpio = contenido.trim();
        if (limpio.length() > 1000) {
            throw new ExcepcionNegocio("El mensaje no puede superar los 1000 caracteres.");
        }
        Mensaje mensaje = new Mensaje(this, remitente, limpio);
        this.mensajes.add(mensaje);
        return mensaje;
    }

    public Long getIdChat() {
        return idChat;
    }

    public Solicitud getSolicitud() {
        return solicitud;
    }

    public OffsetDateTime getFechaCreacion() {
        return fechaCreacion;
    }

    public List<Mensaje> getMensajes() {
        return mensajes;
    }
}
