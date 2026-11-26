package co.edu.udea.alamano.comun.auditoria;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;

@Entity
@Table(name = "auditoria_administrativa")
public class AuditoriaAdministrativa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_auditoria")
    private Long idAuditoria;

    @Column(name = "id_administrador", nullable = false)
    private Long idAdministrador;

    @Column(name = "accion", nullable = false, length = 80)
    private String accion;

    @Column(name = "entidad_afectada", nullable = false, length = 80)
    private String entidadAfectada;

    @Column(name = "id_entidad", nullable = false)
    private Long idEntidad;

    @Column(name = "detalle", length = 1000)
    private String detalle;

    @Column(name = "fecha_accion", nullable = false)
    private OffsetDateTime fechaAccion;

    protected AuditoriaAdministrativa() {
    }

    public AuditoriaAdministrativa(Long idAdministrador, String accion, String entidadAfectada, Long idEntidad, String detalle) {
        this.idAdministrador = idAdministrador;
        this.accion = accion;
        this.entidadAfectada = entidadAfectada;
        this.idEntidad = idEntidad;
        this.detalle = detalle;
        this.fechaAccion = OffsetDateTime.now();
    }

    public Long getIdAuditoria() {
        return idAuditoria;
    }

    public Long getIdAdministrador() {
        return idAdministrador;
    }

    public String getAccion() {
        return accion;
    }

    public String getEntidadAfectada() {
        return entidadAfectada;
    }

    public Long getIdEntidad() {
        return idEntidad;
    }

    public String getDetalle() {
        return detalle;
    }

    public OffsetDateTime getFechaAccion() {
        return fechaAccion;
    }
}
