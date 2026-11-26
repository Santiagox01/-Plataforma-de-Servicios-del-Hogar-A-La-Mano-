package co.edu.udea.alamano.autenticacion_seguridad.seguridad;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;

@Entity
@Table(name = "tokens_revocados")
public class TokenRevocado {

    @Id
    @Column(name = "jti", length = 100)
    private String jti;

    @Column(name = "fecha_expiracion", nullable = false)
    private OffsetDateTime fechaExpiracion;

    @Column(name = "fecha_revocacion", nullable = false)
    private OffsetDateTime fechaRevocacion;

    protected TokenRevocado() {
    }

    public TokenRevocado(String jti, OffsetDateTime fechaExpiracion) {
        this.jti = jti;
        this.fechaExpiracion = fechaExpiracion;
        this.fechaRevocacion = OffsetDateTime.now();
    }

    public String getJti() {
        return jti;
    }

    public OffsetDateTime getFechaExpiracion() {
        return fechaExpiracion;
    }

    public OffsetDateTime getFechaRevocacion() {
        return fechaRevocacion;
    }
}
