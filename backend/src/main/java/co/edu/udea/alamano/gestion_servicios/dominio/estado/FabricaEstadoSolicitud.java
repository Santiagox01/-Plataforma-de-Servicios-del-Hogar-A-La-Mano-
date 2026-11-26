package co.edu.udea.alamano.gestion_servicios.dominio.estado;

import co.edu.udea.alamano.gestion_servicios.dominio.EstadoSolicitud;

import java.util.EnumMap;
import java.util.Map;

public final class FabricaEstadoSolicitud {

    private static final Map<EstadoSolicitud, IEstadoSolicitudState> ESTADOS = new EnumMap<>(EstadoSolicitud.class);

    static {
        ESTADOS.put(EstadoSolicitud.PENDIENTE, new EstadoPendienteState());
        ESTADOS.put(EstadoSolicitud.ACEPTADA, new EstadoAceptadaState());
        ESTADOS.put(EstadoSolicitud.RECHAZADA, new EstadoRechazadaState());
        ESTADOS.put(EstadoSolicitud.EN_CAMINO, new EstadoEnCaminoState());
        ESTADOS.put(EstadoSolicitud.EN_PROGRESO, new EstadoEnProgresoState());
        ESTADOS.put(EstadoSolicitud.FINALIZADA, new EstadoFinalizadaState());
        ESTADOS.put(EstadoSolicitud.EN_DISPUTA, new EstadoEnDisputaState());
        ESTADOS.put(EstadoSolicitud.CANCELADA, new EstadoCanceladaState());
        ESTADOS.put(EstadoSolicitud.PAGADA, new EstadoPagadaState());
    }

    private FabricaEstadoSolicitud() {
    }

    public static IEstadoSolicitudState obtener(EstadoSolicitud estado) {
        IEstadoSolicitudState instancia = ESTADOS.get(estado);
        if (instancia == null) {
            throw new IllegalArgumentException("Estado de solicitud no soportado: " + estado);
        }
        return instancia;
    }
}
