package co.edu.udea.alamano.gestion_servicios.aplicacion;

import java.util.List;

/**
 * Puerto de consulta de reputacion para mantener desacoplado el modulo de busqueda
 * hasta la implementacion completa de Resena en el Sprint 2.
 */
public interface IConsultaReputacion {

    ResumenReputacion obtenerResumen(Long idProfesional);

    record ComentarioPublico(String autor, int estrellas, String comentario, String fecha) {
    }

    record ResumenReputacion(double calificacionPromedio, int totalResenas, List<ComentarioPublico> ultimosComentarios) {
    }
}
