package co.edu.udea.alamano.gestion_servicios.aplicacion;

import co.edu.udea.alamano.gestion_servicios.dominio.Solicitud;

/**
 * Puerto para crear o enlazar un caso de soporte cuando el cliente rechaza el precio final (EN_DISPUTA).
 */
public interface IRegistradorDisputaSoporte {

    String crearCasoPorDisputa(Solicitud solicitud, String motivo);
}
