package co.edu.udea.alamano.gestion_servicios.infraestructura;

import co.edu.udea.alamano.gestion_servicios.dominio.Solicitud;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class AdaptadorSolicitudRepositorio implements ISolicitudRepositorio {

    private final JpaSolicitudRepositorio jpaSolicitudRepositorio;

    public AdaptadorSolicitudRepositorio(JpaSolicitudRepositorio jpaSolicitudRepositorio) {
        this.jpaSolicitudRepositorio = jpaSolicitudRepositorio;
    }

    @Override
    public Optional<Solicitud> buscarPorId(Long idSolicitud) {
        return jpaSolicitudRepositorio.findById(idSolicitud);
    }

    @Override
    public Optional<Solicitud> buscarDetallePorId(Long idSolicitud) {
        return jpaSolicitudRepositorio.buscarDetallePorId(idSolicitud);
    }

    @Override
    public List<Solicitud> listarPorCliente(Long idCliente) {
        return jpaSolicitudRepositorio.listarPorCliente(idCliente);
    }

    @Override
    public List<Solicitud> listarPorProfesional(Long idProfesional) {
        return jpaSolicitudRepositorio.listarPorProfesional(idProfesional);
    }

    @Override
    public Solicitud guardar(Solicitud solicitud) {
        return jpaSolicitudRepositorio.saveAndFlush(solicitud);
    }
}
