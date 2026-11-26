package co.edu.udea.alamano.autenticacion_seguridad.infraestructura;

import co.edu.udea.alamano.gestion_servicios.infraestructura.ISolicitudRepositorio;
import org.springframework.stereotype.Component;

/**
 * Fabrica centralizada de repositorios (patron GoF Factory Method).
 */
@Component
public class RepositorioFactory {

    private final IUsuarioRepositorio usuarioRepositorio;
    private final ClienteRepositorio clienteRepositorio;
    private final ProfesionalRepositorio profesionalRepositorio;
    private final AdministradorRepositorio administradorRepositorio;
    private final ISolicitudRepositorio solicitudRepositorio;

    public RepositorioFactory(IUsuarioRepositorio usuarioRepositorio,
                              ClienteRepositorio clienteRepositorio,
                              ProfesionalRepositorio profesionalRepositorio,
                              AdministradorRepositorio administradorRepositorio,
                              ISolicitudRepositorio solicitudRepositorio) {
        this.usuarioRepositorio = usuarioRepositorio;
        this.clienteRepositorio = clienteRepositorio;
        this.profesionalRepositorio = profesionalRepositorio;
        this.administradorRepositorio = administradorRepositorio;
        this.solicitudRepositorio = solicitudRepositorio;
    }

    public IUsuarioRepositorio crearUsuarioRepositorio() {
        return usuarioRepositorio;
    }

    public ClienteRepositorio crearClienteRepositorio() {
        return clienteRepositorio;
    }

    public ProfesionalRepositorio crearProfesionalRepositorio() {
        return profesionalRepositorio;
    }

    public AdministradorRepositorio crearAdministradorRepositorio() {
        return administradorRepositorio;
    }

    public ISolicitudRepositorio crearSolicitudRepositorio() {
        return solicitudRepositorio;
    }
}
