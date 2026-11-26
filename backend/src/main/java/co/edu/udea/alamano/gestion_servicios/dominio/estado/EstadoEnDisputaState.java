package co.edu.udea.alamano.gestion_servicios.dominio.estado;

import co.edu.udea.alamano.gestion_servicios.dominio.EstadoSolicitud;

public class EstadoEnDisputaState implements IEstadoSolicitudState {

    @Override
    public EstadoSolicitud getEstado() {
        return EstadoSolicitud.EN_DISPUTA;
    }

    @Override
    public boolean chatHabilitado() {
        return true;
    }

    @Override
    public boolean puedeLiberarCustodia() {
        return false;
    }
}
