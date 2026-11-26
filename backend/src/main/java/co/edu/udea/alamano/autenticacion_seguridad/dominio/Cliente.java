package co.edu.udea.alamano.autenticacion_seguridad.dominio;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.Table;

@Entity
@Table(name = "clientes")
@PrimaryKeyJoinColumn(name = "id_usuario")
public class Cliente extends Usuario {

    @Column(name = "ubicacion", nullable = false, length = 255)
    private String ubicacion;

    @Column(name = "latitud")
    private Double latitud;

    @Column(name = "longitud")
    private Double longitud;

    protected Cliente() {
    }

    public Cliente(String nombre,
                   String correo,
                   String celular,
                   String contrasenaHash,
                   String ubicacion,
                   Double latitud,
                   Double longitud) {
        super(nombre, correo, celular, contrasenaHash, RolUsuario.CLIENTE);
        this.ubicacion = ubicacion;
        this.latitud = latitud;
        this.longitud = longitud;
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
}
