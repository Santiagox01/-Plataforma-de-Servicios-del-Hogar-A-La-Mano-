package co.edu.udea.alamano.cotizacion_tarificacion;

import co.edu.udea.alamano.autenticacion_seguridad.dominio.Profesional;
import co.edu.udea.alamano.autenticacion_seguridad.infraestructura.ProfesionalRepositorio;
import co.edu.udea.alamano.autenticacion_seguridad.infraestructura.RepositorioFactory;
import co.edu.udea.alamano.comun.configuracion.TarifaConfig;
import co.edu.udea.alamano.comun.excepcion.ExcepcionNegocio;
import co.edu.udea.alamano.cotizacion_tarificacion.aplicacion.CalculadorTarifa;
import co.edu.udea.alamano.cotizacion_tarificacion.aplicacion.ServicioTarifasProfesional;
import co.edu.udea.alamano.cotizacion_tarificacion.dominio.CategoriaOficio;
import co.edu.udea.alamano.cotizacion_tarificacion.dominio.ResultadoCotizacion;
import co.edu.udea.alamano.cotizacion_tarificacion.dominio.Servicio;
import co.edu.udea.alamano.cotizacion_tarificacion.dominio.TarifaEstandarStrategy;
import co.edu.udea.alamano.cotizacion_tarificacion.dominio.TarifaUrgenciaStrategy;
import co.edu.udea.alamano.cotizacion_tarificacion.dominio.TarificadorContext;
import co.edu.udea.alamano.cotizacion_tarificacion.dto.ConfigurarTarifaDTO;
import co.edu.udea.alamano.cotizacion_tarificacion.infraestructura.ServicioRepositorio;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CalculadorTarifaTest {

    @Mock
    private ServicioRepositorio servicioRepositorio;

    @Mock
    private RepositorioFactory repositorioFactory;

    @Mock
    private ProfesionalRepositorio profesionalRepositorio;

    private TarifaConfig tarifaConfig;
    private TarificadorContext tarificadorContext;
    private CalculadorTarifa calculadorTarifa;

    @BeforeEach
    void setUp() {
        tarifaConfig = new TarifaConfig();
        tarificadorContext = new TarificadorContext(List.of(
                new TarifaEstandarStrategy(tarifaConfig),
                new TarifaUrgenciaStrategy(tarifaConfig)
        ));
        calculadorTarifa = new CalculadorTarifa(servicioRepositorio, tarificadorContext, tarifaConfig);
    }

    @Test
    @DisplayName("PU-01: base 50.000, min 40.000, max 60.000, urgente acota al máximo (60.000)")
    void debeAcotarPrecioUrgenteAlMaximoPermitido() {
        Servicio servicio = new Servicio(mock(Profesional.class), new CategoriaOficio("Electricidad", "Desc"));
        servicio.configurarTarifa(
                new BigDecimal("50000"),
                new BigDecimal("40000"),
                new BigDecimal("60000")
        );

        ResultadoCotizacion urgente = calculadorTarifa.calcularDetalle(servicio, true);
        assertThat(urgente.precioSinAcotar()).isEqualByComparingTo("65000.00");
        assertThat(urgente.precioEstimado()).isEqualByComparingTo("60000.00");
        assertThat(urgente.recargoUrgenciaAplicado()).isTrue();
        assertThat(urgente.acotadoAlMaximo()).isTrue();

        BigDecimal normal = calculadorTarifa.calcularPrecioEstimado(servicio, false);
        assertThat(normal).isEqualByComparingTo("50000.00");
    }

    @Test
    @DisplayName("HU09: Servicio sin tarifa configurada lanza error claro")
    void servicioSinTarifaLanzaError() {
        Servicio sinTarifa = new Servicio(mock(Profesional.class), new CategoriaOficio("Cerrajería", "Desc"));
        assertThatThrownBy(() -> calculadorTarifa.calcularPrecioEstimado(sinTarifa, true))
                .isInstanceOf(ExcepcionNegocio.class)
                .hasMessageContaining("no ha configurado tarifas");
    }

    @Test
    @DisplayName("PU-04: configurarTarifa con min > max lanza error y no guarda en repositorio")
    void configurarTarifaConMinMayorAMaxNoGuarda() {
        Profesional pro = new Profesional(
                "Juan", "juan@alamano.co", "3101112233", "hash",
                "Medellín", 6.2, -75.5, "doc.pdf", List.of()
        );
        Servicio servicio = new Servicio(pro, new CategoriaOficio("Plomería", "Desc"));

        when(repositorioFactory.crearProfesionalRepositorio()).thenReturn(profesionalRepositorio);
        when(profesionalRepositorio.findById(1L)).thenReturn(Optional.of(pro));
        when(servicioRepositorio.findByProfesionalIdUsuarioAndCategoriaOficioIdCategoria(any(), any()))
                .thenReturn(Optional.of(servicio));

        ServicioTarifasProfesional gestorTarifas = new ServicioTarifasProfesional(servicioRepositorio, repositorioFactory);
        ConfigurarTarifaDTO invalido = new ConfigurarTarifaDTO(
                2L,
                new BigDecimal("45000"),
                new BigDecimal("50000"),
                new BigDecimal("40000")
        );

        assertThatThrownBy(() -> gestorTarifas.configurarTarifa(1L, invalido))
                .isInstanceOf(ExcepcionNegocio.class)
                .hasMessageContaining("estrictamente menor");

        verify(servicioRepositorio, never()).save(any());
        assertThat(servicio.tieneTarifaConfigurada()).isFalse();
    }
}
