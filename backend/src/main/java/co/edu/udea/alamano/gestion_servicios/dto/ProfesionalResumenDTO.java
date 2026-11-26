package co.edu.udea.alamano.gestion_servicios.dto;

import java.math.BigDecimal;

public record ProfesionalResumenDTO(
        Long idProfesional,
        String nombre,
        String ubicacion,
        double distanciaKm,
        double calificacionPromedio,
        int totalResenas,
        boolean verificado,
        String selloVerificacion,
        Long idServicio,
        BigDecimal tarifaBase,
        BigDecimal precioMin,
        BigDecimal precioMax
) {
}
