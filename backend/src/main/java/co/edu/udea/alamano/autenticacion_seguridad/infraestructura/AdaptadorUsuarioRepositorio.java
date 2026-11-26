package co.edu.udea.alamano.autenticacion_seguridad.infraestructura;

import co.edu.udea.alamano.autenticacion_seguridad.dominio.Usuario;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class AdaptadorUsuarioRepositorio implements IUsuarioRepositorio {

    private final JpaUsuarioRepositorio jpaUsuarioRepositorio;

    public AdaptadorUsuarioRepositorio(JpaUsuarioRepositorio jpaUsuarioRepositorio) {
        this.jpaUsuarioRepositorio = jpaUsuarioRepositorio;
    }

    @Override
    public Optional<Usuario> buscarPorId(Long id) {
        return jpaUsuarioRepositorio.findById(id);
    }

    @Override
    public Optional<Usuario> buscarPorCorreo(String correo) {
        return jpaUsuarioRepositorio.findByCorreoIgnoreCase(correo);
    }

    @Override
    public boolean existePorCorreo(String correo) {
        return jpaUsuarioRepositorio.existsByCorreoIgnoreCase(correo);
    }

    @Override
    public Usuario guardar(Usuario usuario) {
        return jpaUsuarioRepositorio.save(usuario);
    }
}
