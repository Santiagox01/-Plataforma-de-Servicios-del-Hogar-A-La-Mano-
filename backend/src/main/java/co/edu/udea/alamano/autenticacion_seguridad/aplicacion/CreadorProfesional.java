package co.edu.udea.alamano.autenticacion_seguridad.aplicacion;

import co.edu.udea.alamano.autenticacion_seguridad.dominio.Profesional;
import co.edu.udea.alamano.autenticacion_seguridad.dominio.Usuario;
import co.edu.udea.alamano.autenticacion_seguridad.infraestructura.IUsuarioRepositorio;
import co.edu.udea.alamano.comun.excepcion.ExcepcionNegocio;
import co.edu.udea.alamano.comun.validacion.ValidadorDatosComunes;
import co.edu.udea.alamano.cotizacion_tarificacion.dominio.CategoriaOficio;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class CreadorProfesional extends CreadorUsuario {

    public CreadorProfesional(IUsuarioRepositorio usuarioRepositorio,
                              PasswordEncoder passwordEncoder,
                              ValidadorDatosComunes validador) {
        super(usuarioRepositorio, passwordEncoder, validador);
    }

    @Override
    protected Usuario crearUsuario(DatosRegistroUsuario datos, String contrasenaHash) {
        if (datos.rutaDocumentoIdentidad() == null || datos.rutaDocumentoIdentidad().isBlank()) {
            throw new ExcepcionNegocio("El documento de identidad es obligatorio para registrar un profesional.");
        }
        if (datos.categorias() == null || datos.categorias().isEmpty()) {
            throw new ExcepcionNegocio("Debes seleccionar al menos una categoría de oficio.");
        }

        Profesional profesional = new Profesional(
                datos.nombre(),
                datos.correo(),
                datos.celular(),
                contrasenaHash,
                datos.ubicacion(),
                datos.latitud(),
                datos.longitud(),
                datos.rutaDocumentoIdentidad(),
                datos.rutasCertificaciones()
        );
        for (CategoriaOficio categoria : datos.categorias()) {
            profesional.agregarServicio(categoria);
        }
        return profesional;
    }
}
