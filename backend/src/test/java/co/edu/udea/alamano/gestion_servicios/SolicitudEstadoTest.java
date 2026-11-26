package co.edu.udea.alamano.gestion_servicios;

import co.edu.udea.alamano.autenticacion_seguridad.dominio.Cliente;
import co.edu.udea.alamano.autenticacion_seguridad.dominio.Profesional;
import co.edu.udea.alamano.comun.excepcion.ExcepcionConflicto;
import co.edu.udea.alamano.comun.excepcion.ExcepcionNegocio;
import co.edu.udea.alamano.cotizacion_tarificacion.dominio.Servicio;
import co.edu.udea.alamano.gestion_servicios.dominio.EstadoSolicitud;
import co.edu.udea.alamano.gestion_servicios.dominio.Solicitud;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

class SolicitudEstadoTest {

    private Solicitud crearSolicitudPendiente() {
        return new Solicitud(
                mock(Cliente.class),
                mock(Profesional.class),
                mock(Servicio.class),
                "Fuga en tubería",
                List.of(),
                false,
                new BigDecimal("45000.00")
        );
    }

    @Test
    @DisplayName("Máquina de estados State rechaza transiciones inválidas y protege inmutabilidad de precioFinal")
    void validaTransicionesYPrecioFinalInmutable() {
        Solicitud solicitud = crearSolicitudPendiente();
        assertThat(solicitud.getEstadoSolicitud()).isEqualTo(EstadoSolicitud.PENDIENTE);
        assertThat(solicitud.chatHabilitado()).isFalse();
        assertThat(solicitud.puedeLiberarCustodia()).isFalse();

        assertThatThrownBy(() -> solicitud.registrarPrecioFinal(new BigDecimal("50000")))
                .isInstanceOf(ExcepcionNegocio.class);
        assertThatThrownBy(() -> solicitud.resenar(5, "Bien"))
                .isInstanceOf(ExcepcionNegocio.class);

        solicitud.aceptar();
        assertThat(solicitud.getEstadoSolicitud()).isEqualTo(EstadoSolicitud.ACEPTADA);
        assertThat(solicitud.chatHabilitado()).isTrue();

        assertThatThrownBy(solicitud::aceptar)
                .isInstanceOf(ExcepcionConflicto.class);
        assertThatThrownBy(() -> solicitud.rechazar("No puedo"))
                .isInstanceOf(ExcepcionNegocio.class);

        solicitud.registrarPrecioFinal(new BigDecimal("52000"));
        assertThatThrownBy(() -> solicitud.registrarPrecioFinal(new BigDecimal("60000")))
                .isInstanceOf(ExcepcionNegocio.class)
                .hasMessageContaining("inmutable");

        solicitud.confirmarPrecioFinal();
        assertThat(solicitud.getEstadoSolicitud()).isEqualTo(EstadoSolicitud.FINALIZADA);
        assertThat(solicitud.puedeResenarse()).isTrue();
        assertThat(solicitud.puedeLiberarCustodia()).isFalse();

        solicitud.marcarPagada();
        assertThat(solicitud.getEstadoSolicitud()).isEqualTo(EstadoSolicitud.PAGADA);
        assertThat(solicitud.puedeLiberarCustodia()).isTrue();
        assertThat(solicitud.puedeLiberarCustodia(true)).isFalse();
    }

    @Test
    @DisplayName("Rechazar precio final pasa a EN_DISPUTA y bloquea liberación de custodia")
    void rechazoPrecioFinalPasaADisputa() {
        Solicitud solicitud = crearSolicitudPendiente();
        solicitud.aceptar();
        solicitud.registrarPrecioFinal(new BigDecimal("85000"));
        solicitud.rechazarPrecioFinal();

        assertThat(solicitud.getEstadoSolicitud()).isEqualTo(EstadoSolicitud.EN_DISPUTA);
        assertThat(solicitud.puedeLiberarCustodia()).isFalse();
        assertThat(solicitud.puedeResenarse()).isFalse();
    }
}
