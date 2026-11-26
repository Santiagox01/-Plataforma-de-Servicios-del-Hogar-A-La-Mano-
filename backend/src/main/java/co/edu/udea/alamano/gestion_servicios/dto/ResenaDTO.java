package co.edu.udea.alamano.gestion_servicios.dto;

import java.time.OffsetDateTime;

public record ResenaDTO(
        Long idResena,
        Long idSolicitud,
        Long idProfesional,
        int estrellas,
        String comentario,
        OffsetDateTime fechaResena,
        double nuevoPromedioProfesional,
        int totalResenasProfesional
) {
}
