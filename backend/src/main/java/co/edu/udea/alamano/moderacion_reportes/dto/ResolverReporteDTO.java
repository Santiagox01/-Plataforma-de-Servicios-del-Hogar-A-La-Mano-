package co.edu.udea.alamano.moderacion_reportes.dto;

import co.edu.udea.alamano.moderacion_reportes.dominio.AccionModeracion;
import co.edu.udea.alamano.moderacion_reportes.dominio.DecisionPagoModeracion;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ResolverReporteDTO(
        @NotNull(message = "Debes indicar la acción de moderación a aplicar.")
        AccionModeracion accion,
        @NotBlank(message = "La justificación es obligatoria para dejar trazabilidad.")
        String justificacion,
        Integer diasSuspension,
        DecisionPagoModeracion decisionPago
) {
}
