package co.edu.udea.alamano.gestion_servicios.dominio.estado;

import co.edu.udea.alamano.comun.excepcion.ExcepcionNegocio;
import co.edu.udea.alamano.gestion_servicios.dominio.EstadoSolicitud;
import co.edu.udea.alamano.gestion_servicios.dominio.Solicitud;

import java.math.BigDecimal;

/**
 * Patron GoF State para gobernar las transiciones del ciclo de vida de Solicitud.
 */
public interface IEstadoSolicitudState {

    EstadoSolicitud getEstado();

    default void aceptar(Solicitud solicitud) {
        throw new ExcepcionNegocio("No es posible aceptar la solicitud en estado " + getEstado() + ".");
    }

    default void rechazar(Solicitud solicitud, String motivo) {
        throw new ExcepcionNegocio("No es posible rechazar la solicitud en estado " + getEstado() + ".");
    }

    default void marcarEnCamino(Solicitud solicitud) {
        throw new ExcepcionNegocio("No es posible pasar a EN_CAMINO desde el estado " + getEstado() + ".");
    }

    default void iniciarProgreso(Solicitud solicitud) {
        throw new ExcepcionNegocio("No es posible pasar a EN_PROGRESO desde el estado " + getEstado() + ".");
    }

    default void registrarPrecioFinal(Solicitud solicitud, BigDecimal precioFinal) {
        throw new ExcepcionNegocio("Solo se puede registrar el precio final cuando la solicitud fue aceptada o está en progreso.");
    }

    default void confirmarPrecioFinal(Solicitud solicitud) {
        throw new ExcepcionNegocio("No es posible confirmar el precio final en estado " + getEstado() + ".");
    }

    default void rechazarPrecioFinal(Solicitud solicitud) {
        throw new ExcepcionNegocio("No es posible rechazar el precio final en estado " + getEstado() + ".");
    }

    default void marcarPagada(Solicitud solicitud) {
        throw new ExcepcionNegocio("Solo se pueden marcar como pagadas solicitudes finalizadas.");
    }

    default boolean chatHabilitado() {
        return false;
    }

    default boolean puedeResenarse() {
        return false;
    }

    default boolean puedeLiberarCustodia() {
        return false;
    }
}
