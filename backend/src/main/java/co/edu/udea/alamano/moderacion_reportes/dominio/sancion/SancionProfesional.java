package co.edu.udea.alamano.moderacion_reportes.dominio.sancion;

import co.edu.udea.alamano.autenticacion_seguridad.dominio.RolUsuario;
import co.edu.udea.alamano.autenticacion_seguridad.dominio.Usuario;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;

@Component
public class SancionProfesional implements ISancionable {

    @Override
    public RolUsuario getRolSoportado() {
        return RolUsuario.PROFESIONAL;
    }

    @Override
    public String aplicarAdvertencia(Usuario usuario, String justificacion) {
        return "Advertencia formal registrada en el expediente del profesional (" + justificacion + ").";
    }

    @Override
    public String aplicarSuspensionTemporal(Usuario usuario, OffsetDateTime hasta, String justificacion) {
        usuario.suspenderTemporalmente(hasta, justificacion);
        return "Cuenta profesional suspendida hasta " + hasta.toLocalDate()
                + " y ocultada de los resultados de búsqueda.";
    }

    @Override
    public String aplicarDesactivacion(Usuario usuario, String justificacion) {
        usuario.desactivarCuenta(justificacion);
        return "Cuenta profesional desactivada permanentemente y removida del directorio.";
    }
}
