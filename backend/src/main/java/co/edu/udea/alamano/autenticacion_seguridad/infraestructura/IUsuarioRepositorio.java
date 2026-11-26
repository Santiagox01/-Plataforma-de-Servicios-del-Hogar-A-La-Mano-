package co.edu.udea.alamano.autenticacion_seguridad.infraestructura;

import co.edu.udea.alamano.autenticacion_seguridad.dominio.Usuario;

import java.util.Optional;

/**
 * Puerto de dominio para acceso a usuarios (patron GRASP Indireccion y SOLID DIP).
 */
public interface IUsuarioRepositorio {

    Optional<Usuario> buscarPorId(Long id);

    Optional<Usuario> buscarPorCorreo(String correo);

    boolean existePorCorreo(String correo);

    Usuario guardar(Usuario usuario);
}
