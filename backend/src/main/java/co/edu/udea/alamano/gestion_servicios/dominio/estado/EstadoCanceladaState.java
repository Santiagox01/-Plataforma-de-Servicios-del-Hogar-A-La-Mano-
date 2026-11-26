package co.edu.udea.alamano.gestion_servicios.dominio.estado;

import co.edu.udea.alamano.gestion_servicios.dominio.EstadoSolicitud;

public class EstadoCanceladaState implements IEstadoSolicitudState {

    @Override
    public EstadoSolicitud getEstado() {
        return EstadoSolicitud.CANCELADA;
    }
}
