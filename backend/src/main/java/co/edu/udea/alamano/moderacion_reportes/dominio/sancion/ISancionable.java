package co.edu.udea.alamano.moderacion_reportes.dominio.sancion;

import co.edu.udea.alamano.autenticacion_seguridad.dominio.RolUsuario;
import co.edu.udea.alamano.autenticacion_seguridad.dominio.Usuario;

import java.time.OffsetDateTime;

/**
 * Estrategia polimorfica de sancion por rol (patron GRASP Polymorphism, evita condicionales por rol).
 */
public interface ISancionable {

    RolUsuario getRolSoportado();

    String aplicarAdvertencia(Usuario usuario, String justificacion);

    String aplicarSuspensionTemporal(Usuario usuario, OffsetDateTime hasta, String justificacion);

    String aplicarDesactivacion(Usuario usuario, String justificacion);
}
