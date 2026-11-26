package co.edu.udea.alamano.gestion_servicios.dominio.estado;

import co.edu.udea.alamano.gestion_servicios.dominio.EstadoSolicitud;
import co.edu.udea.alamano.gestion_servicios.dominio.Solicitud;

public class EstadoFinalizadaState implements IEstadoSolicitudState {

    @Override
    public EstadoSolicitud getEstado() {
        return EstadoSolicitud.FINALIZADA;
    }

    @Override
    public void marcarPagada(Solicitud solicitud) {
        solicitud.cambiarEstadoInterno(EstadoSolicitud.PAGADA);
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
        return false;
    }
}
