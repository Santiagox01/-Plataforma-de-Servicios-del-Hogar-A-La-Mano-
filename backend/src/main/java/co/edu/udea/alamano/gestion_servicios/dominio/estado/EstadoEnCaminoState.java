package co.edu.udea.alamano.gestion_servicios.dominio.estado;

import co.edu.udea.alamano.gestion_servicios.dominio.EstadoSolicitud;
import co.edu.udea.alamano.gestion_servicios.dominio.Solicitud;

import java.math.BigDecimal;

public class EstadoEnCaminoState implements IEstadoSolicitudState {

    @Override
    public EstadoSolicitud getEstado() {
        return EstadoSolicitud.EN_CAMINO;
    }

    @Override
    public void iniciarProgreso(Solicitud solicitud) {
        solicitud.cambiarEstadoInterno(EstadoSolicitud.EN_PROGRESO);
    }

    @Override
    public void registrarPrecioFinal(Solicitud solicitud, BigDecimal precioFinal) {
        solicitud.asignarPrecioFinalInmutable(precioFinal);
    }

    @Override
    public void confirmarPrecioFinal(Solicitud solicitud) {
        solicitud.confirmarYFnalizarInterno();
    }

    @Override
    public void rechazarPrecioFinal(Solicitud solicitud) {
        solicitud.rechazarPrecioYDisputarInterno();
    }

    @Override
    public boolean chatHabilitado() {
        return true;
    }
}
