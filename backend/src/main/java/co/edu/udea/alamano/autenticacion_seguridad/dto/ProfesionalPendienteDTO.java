package co.edu.udea.alamano.autenticacion_seguridad.dto;

import co.edu.udea.alamano.autenticacion_seguridad.dominio.EstadoVerificacion;

import java.time.OffsetDateTime;
import java.util.List;

public record ProfesionalPendienteDTO(
        Long idProfesional,
        String nombre,
        String correo,
        String celular,
        String ubicacion,
        EstadoVerificacion estadoVerificacion,
        OffsetDateTime fechaRegistro,
        List<String> categorias,
        int cantidadCertificaciones
) {
}
