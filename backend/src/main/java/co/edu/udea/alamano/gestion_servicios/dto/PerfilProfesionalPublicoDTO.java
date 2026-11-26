package co.edu.udea.alamano.gestion_servicios.dto;

import co.edu.udea.alamano.gestion_servicios.aplicacion.IConsultaReputacion;

import java.math.BigDecimal;
import java.util.List;

public record PerfilProfesionalPublicoDTO(
        Long idProfesional,
        String nombre,
        String fotoPerfil,
        String ubicacion,
        boolean verificado,
        String selloVerificacion,
        boolean disponible,
        double calificacionPromedio,
        int totalResenas,
        List<ServicioOfrecidoDTO> servicios,
        List<IConsultaReputacion.ComentarioPublico> ultimosComentarios
) {
    public record ServicioOfrecidoDTO(
            Long idServicio,
            Long idCategoria,
            String nombreCategoria,
            BigDecimal tarifaBase,
            BigDecimal precioMin,
            BigDecimal precioMax
    ) {
    }
}
