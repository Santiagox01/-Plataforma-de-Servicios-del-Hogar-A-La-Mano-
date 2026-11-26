package co.edu.udea.alamano.gestion_servicios.dto;

import java.util.List;

public record RespuestaRechazoSolicitudDTO(
        SolicitudDTO solicitud,
        List<ProfesionalResumenDTO> profesionalesSugeridos
) {
}
