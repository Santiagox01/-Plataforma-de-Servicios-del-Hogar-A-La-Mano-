package co.edu.udea.alamano.gestion_servicios.dto;

import co.edu.udea.alamano.gestion_servicios.dominio.EstadoSolicitud;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

public record SolicitudDTO(
        Long idSolicitud,
        Long idCliente,
        String nombreCliente,
        Long idProfesional,
        String nombreProfesional,
        Long idServicio,
        Long idCategoria,
        String nombreCategoria,
        String descripcion,
        List<String> urlsFotos,
        EstadoSolicitud estadoSolicitud,
        String motivoRechazo,
        boolean esUrgente,
        BigDecimal precioEstimado,
        BigDecimal precioFinal,
        boolean precioFinalConfirmado,
        boolean chatHabilitado,
        boolean puedeResenarse,
        boolean yaResenada,
        OffsetDateTime fechaSolicitud,
        OffsetDateTime fechaFinalizacion,
        Long idPago,
        String estadoPago,
        boolean puedePagar,
        boolean puedeLiberarCustodia,
        boolean tieneReporteAbierto
) {
}
