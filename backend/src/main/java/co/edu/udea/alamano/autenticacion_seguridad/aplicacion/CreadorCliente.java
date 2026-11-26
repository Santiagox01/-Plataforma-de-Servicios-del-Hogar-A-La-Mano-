package co.edu.udea.alamano.autenticacion_seguridad.aplicacion;

import co.edu.udea.alamano.autenticacion_seguridad.dominio.Cliente;
import co.edu.udea.alamano.autenticacion_seguridad.dominio.Usuario;
import co.edu.udea.alamano.autenticacion_seguridad.infraestructura.IUsuarioRepositorio;
import co.edu.udea.alamano.comun.validacion.ValidadorDatosComunes;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class CreadorCliente extends CreadorUsuario {

    public CreadorCliente(IUsuarioRepositorio usuarioRepositorio,
                          PasswordEncoder passwordEncoder,
                          ValidadorDatosComunes validador) {
        super(usuarioRepositorio, passwordEncoder, validador);
    }

    @Override
    protected Usuario crearUsuario(DatosRegistroUsuario datos, String contrasenaHash) {
        return new Cliente(
                datos.nombre(),
                datos.correo(),
                datos.celular(),
                contrasenaHash,
                datos.ubicacion(),
                datos.latitud(),
                datos.longitud()
        );
    }
}
