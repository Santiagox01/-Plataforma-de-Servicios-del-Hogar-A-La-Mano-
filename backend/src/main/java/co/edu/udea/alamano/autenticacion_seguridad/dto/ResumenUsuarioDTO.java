package co.edu.udea.alamano.autenticacion_seguridad.dto;

import co.edu.udea.alamano.autenticacion_seguridad.dominio.EstadoCuenta;
import co.edu.udea.alamano.autenticacion_seguridad.dominio.EstadoVerificacion;
import co.edu.udea.alamano.autenticacion_seguridad.dominio.RolUsuario;

public record ResumenUsuarioDTO(
        Long idUsuario,
        String nombre,
        String correo,
        String celular,
        RolUsuario rol,
        EstadoCuenta estadoCuenta,
        EstadoVerificacion estadoVerificacion,
        String motivoRechazo,
        Boolean disponible,
        String ubicacion,
        Double latitud,
        Double longitud
) {
}
