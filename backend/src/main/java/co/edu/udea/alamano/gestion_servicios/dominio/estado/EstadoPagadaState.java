package co.edu.udea.alamano.gestion_servicios.dominio.estado;

import co.edu.udea.alamano.gestion_servicios.dominio.EstadoSolicitud;

public class EstadoPagadaState implements IEstadoSolicitudState {

    @Override
    public EstadoSolicitud getEstado() {
        return EstadoSolicitud.PAGADA;
    }

    @Override
    public boolean chatHabilitado() {
        return true;
    }

    @Override
    public boolean puedeResenarse() {
        return true;
    }

    @Override
    public boolean puedeLiberarCustodia() {
        return true;
    }
}
