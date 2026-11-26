package co.edu.udea.alamano.gestion_servicios.dominio;

import co.edu.udea.alamano.autenticacion_seguridad.dominio.Cliente;
import co.edu.udea.alamano.autenticacion_seguridad.dominio.Profesional;
import co.edu.udea.alamano.comun.excepcion.ExcepcionNegocio;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;

@Entity
@Table(name = "resenas")
public class Resena {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_resena")
    private Long idResena;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_solicitud", nullable = false, unique = true)
    private Solicitud solicitud;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_cliente", nullable = false)
    private Cliente cliente;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_profesional", nullable = false)
    private Profesional profesional;

    @Column(name = "estrellas", nullable = false)
    private int estrellas;

    @Column(name = "comentario", length = 600)
    private String comentario;

    @Column(name = "fecha_resena", nullable = false)
    private OffsetDateTime fechaResena;

    protected Resena() {
    }

    public Resena(Solicitud solicitud, Cliente cliente, Profesional profesional, int estrellas, String comentario) {
        if (estrellas < 1 || estrellas > 5) {
            throw new ExcepcionNegocio("La calificación debe estar entre 1 y 5 estrellas.");
        }
        this.solicitud = solicitud;
        this.cliente = cliente;
        this.profesional = profesional;
        this.estrellas = estrellas;
        this.comentario = (comentario == null || comentario.isBlank()) ? null : comentario.trim();
        this.fechaResena = OffsetDateTime.now();
    }

    public Long getIdResena() {
        return idResena;
    }

    public Solicitud getSolicitud() {
        return solicitud;
    }

    public Cliente getCliente() {
        return cliente;
    }

    public Profesional getProfesional() {
        return profesional;
    }

    public int getEstrellas() {
        return estrellas;
    }

    public String getComentario() {
        return comentario;
    }

    public OffsetDateTime getFechaResena() {
        return fechaResena;
    }
}
