package co.edu.udea.alamano.autenticacion_seguridad.aplicacion;

import co.edu.udea.alamano.autenticacion_seguridad.dominio.Usuario;
import co.edu.udea.alamano.autenticacion_seguridad.infraestructura.IUsuarioRepositorio;
import co.edu.udea.alamano.comun.excepcion.ExcepcionConflicto;
import co.edu.udea.alamano.comun.excepcion.ExcepcionNegocio;
import co.edu.udea.alamano.comun.validacion.ValidadorDatosComunes;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Patron GoF Factory Method para registro de usuarios.
 */
public abstract class CreadorUsuario {

    protected final IUsuarioRepositorio usuarioRepositorio;
    protected final PasswordEncoder passwordEncoder;
    protected final ValidadorDatosComunes validador;

    protected CreadorUsuario(IUsuarioRepositorio usuarioRepositorio,
                             PasswordEncoder passwordEncoder,
                             ValidadorDatosComunes validador) {
        this.usuarioRepositorio = usuarioRepositorio;
        this.passwordEncoder = passwordEncoder;
        this.validador = validador;
    }

    public Usuario registrar(DatosRegistroUsuario datos) {
        if (datos.nombre() == null || datos.nombre().isBlank()) {
            throw new ExcepcionNegocio("El nombre es obligatorio.");
        }
        String correoNormalizado = validador.normalizarYValidarCorreo(datos.correo());
        if (usuarioRepositorio.existePorCorreo(correoNormalizado)) {
            throw new ExcepcionConflicto("El correo electrónico ya está en uso.");
        }
        String celularValidado = validador.validarCelularColombiano(datos.celular());
        validador.validarContrasena(datos.contrasena());
        validador.validarCoordenadas(datos.latitud(), datos.longitud());

        DatosRegistroUsuario datosLimpios = new DatosRegistroUsuario(
                datos.nombre().trim(),
                correoNormalizado,
                celularValidado,
                datos.contrasena(),
                datos.ubicacion() != null ? datos.ubicacion().trim() : "",
                datos.latitud(),
                datos.longitud(),
                datos.rutaDocumentoIdentidad(),
                datos.rutasCertificaciones(),
                datos.categorias()
        );

        String hash = passwordEncoder.encode(datosLimpios.contrasena());
        Usuario nuevo = crearUsuario(datosLimpios, hash);
        return usuarioRepositorio.guardar(nuevo);
    }

    protected abstract Usuario crearUsuario(DatosRegistroUsuario datos, String contrasenaHash);
}
