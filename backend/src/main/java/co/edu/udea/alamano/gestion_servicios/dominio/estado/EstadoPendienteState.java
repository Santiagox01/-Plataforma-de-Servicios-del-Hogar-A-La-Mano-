package co.edu.udea.alamano.gestion_servicios.dominio.estado;

import co.edu.udea.alamano.gestion_servicios.dominio.EstadoSolicitud;
import co.edu.udea.alamano.gestion_servicios.dominio.Solicitud;

public class EstadoPendienteState implements IEstadoSolicitudState {

    @Override
    public EstadoSolicitud getEstado() {
        return EstadoSolicitud.PENDIENTE;
    }

    @Override
    public void aceptar(Solicitud solicitud) {
        solicitud.cambiarEstadoInterno(EstadoSolicitud.ACEPTADA);
    }

    @Override
    public void rechazar(Solicitud solicitud, String motivo) {
        solicitud.asignarMotivoRechazo(motivo);
        solicitud.cambiarEstadoInterno(EstadoSolicitud.RECHAZADA);
    }
}
