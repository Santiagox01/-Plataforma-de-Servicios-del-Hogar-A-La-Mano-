package co.edu.udea.alamano.autenticacion_seguridad.dominio;

import jakarta.persistence.Entity;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.Table;

@Entity
@Table(name = "administradores")
@PrimaryKeyJoinColumn(name = "id_usuario")
public class Administrador extends Usuario {

    protected Administrador() {
    }

    public Administrador(String nombre, String correo, String celular, String contrasenaHash) {
        super(nombre, correo, celular, contrasenaHash, RolUsuario.ADMINISTRADOR);
    }
}
