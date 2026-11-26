package co.edu.udea.alamano.moderacion_reportes.dominio.sancion;

import co.edu.udea.alamano.autenticacion_seguridad.dominio.RolUsuario;
import co.edu.udea.alamano.autenticacion_seguridad.dominio.Usuario;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;

@Component
public class SancionCliente implements ISancionable {

    @Override
    public RolUsuario getRolSoportado() {
        return RolUsuario.CLIENTE;
    }

    @Override
    public String aplicarAdvertencia(Usuario usuario, String justificacion) {
        return "Advertencia formal registrada sobre la cuenta de cliente (" + justificacion + ").";
    }

    @Override
    public String aplicarSuspensionTemporal(Usuario usuario, OffsetDateTime hasta, String justificacion) {
        usuario.suspenderTemporalmente(hasta, justificacion);
        return "Cuenta de cliente suspendida temporalmente hasta " + hasta.toLocalDate()
                + ", restringiendo inicio de sesión y nuevas solicitudes.";
    }

    @Override
    public String aplicarDesactivacion(Usuario usuario, String justificacion) {
        usuario.desactivarCuenta(justificacion);
        return "Cuenta de cliente desactivada permanentemente por moderación.";
    }
}
