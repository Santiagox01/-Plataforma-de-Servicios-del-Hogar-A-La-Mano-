package co.edu.udea.alamano.autenticacion_seguridad.seguridad;

import co.edu.udea.alamano.autenticacion_seguridad.dominio.Usuario;
import co.edu.udea.alamano.autenticacion_seguridad.infraestructura.IUsuarioRepositorio;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ServicioDetallesUsuario implements UserDetailsService {

    private final IUsuarioRepositorio usuarioRepositorio;

    public ServicioDetallesUsuario(IUsuarioRepositorio usuarioRepositorio) {
        this.usuarioRepositorio = usuarioRepositorio;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String correo) throws UsernameNotFoundException {
        Usuario usuario = usuarioRepositorio.buscarPorCorreo(correo)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado."));
        return new DetallesUsuario(usuario);
    }
}
