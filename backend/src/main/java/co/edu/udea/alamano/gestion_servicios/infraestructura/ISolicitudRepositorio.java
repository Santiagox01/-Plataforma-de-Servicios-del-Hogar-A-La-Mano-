package co.edu.udea.alamano.gestion_servicios.infraestructura;

import co.edu.udea.alamano.gestion_servicios.dominio.Solicitud;

import java.util.List;
import java.util.Optional;

public interface ISolicitudRepositorio {

    Optional<Solicitud> buscarPorId(Long idSolicitud);

    Optional<Solicitud> buscarDetallePorId(Long idSolicitud);

    List<Solicitud> listarPorCliente(Long idCliente);

    List<Solicitud> listarPorProfesional(Long idProfesional);

    Solicitud guardar(Solicitud solicitud);
}
