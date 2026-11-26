package co.edu.udea.alamano.gestion_servicios.dto;

import java.util.List;

public record ResultadoBusquedaProfesionalesDTO(
        List<ProfesionalResumenDTO> resultados,
        int totalResultados,
        double radioAplicadoKm,
        String mensajeSugerencia
) {
}
