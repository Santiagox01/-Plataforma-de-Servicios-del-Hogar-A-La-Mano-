package co.edu.udea.alamano.autenticacion_seguridad.dto;

import java.util.List;

public record UrlDocumentoDTO(
        Long idProfesional,
        String urlDocumentoIdentidad,
        List<String> urlsCertificaciones,
        long expiraEnSegundos
) {
}
