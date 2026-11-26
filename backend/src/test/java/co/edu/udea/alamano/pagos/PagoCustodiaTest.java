package co.edu.udea.alamano.pagos;

import co.edu.udea.alamano.autenticacion_seguridad.dominio.Cliente;
import co.edu.udea.alamano.autenticacion_seguridad.dominio.Profesional;
import co.edu.udea.alamano.autenticacion_seguridad.infraestructura.RepositorioFactory;
import co.edu.udea.alamano.comun.auditoria.ServicioAuditoria;
import co.edu.udea.alamano.comun.configuracion.TarifaConfig;
import co.edu.udea.alamano.comun.excepcion.ExcepcionNegocio;
import co.edu.udea.alamano.comun.notificacion.ServicioNotificaciones;
import co.edu.udea.alamano.cotizacion_tarificacion.dominio.CategoriaOficio;
import co.edu.udea.alamano.cotizacion_tarificacion.dominio.Servicio;
import co.edu.udea.alamano.gestion_servicios.aplicacion.IConsultaReportesAbiertos;
import co.edu.udea.alamano.gestion_servicios.dominio.EstadoSolicitud;
import co.edu.udea.alamano.gestion_servicios.dominio.Solicitud;
import co.edu.udea.alamano.gestion_servicios.infraestructura.ISolicitudRepositorio;
import co.edu.udea.alamano.pagos.aplicacion.ServicioPagos;
import co.edu.udea.alamano.pagos.dominio.DatosTarjetaValidacion;
import co.edu.udea.alamano.pagos.dominio.EstadoPago;
import co.edu.udea.alamano.pagos.dominio.IPasarelaPagos;
import co.edu.udea.alamano.pagos.dominio.MetodoPago;
import co.edu.udea.alamano.pagos.dominio.Pago;
import co.edu.udea.alamano.pagos.dominio.ResultadoTransaccionPasarela;
import co.edu.udea.alamano.pagos.dominio.ResultadoValidacionMetodo;
import co.edu.udea.alamano.pagos.dominio.TipoMetodoPago;
import co.edu.udea.alamano.pagos.dto.ComprobantePagoDTO;
import co.edu.udea.alamano.pagos.infraestructura.MetodoPagoRepositorio;
import co.edu.udea.alamano.pagos.infraestructura.PagoRepositorio;
import co.edu.udea.alamano.pagos.infraestructura.PasarelaSimulada;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.YearMonth;
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
class PagoCustodiaTest {

    @Mock
    private PagoRepositorio pagoRepositorio;

    @Mock
    private MetodoPagoRepositorio metodoPagoRepositorio;

    @Mock
    private RepositorioFactory repositorioFactory;

    @Mock
    private ISolicitudRepositorio solicitudRepositorio;

    @Mock
    private IPasarelaPagos pasarelaPagos;

    @Mock
    private IConsultaReportesAbiertos consultaReportesAbiertos;

    @Mock
    private ServicioNotificaciones servicioNotificaciones;

    @Mock
    private ServicioAuditoria servicioAuditoria;

    private ServicioPagos servicioPagos;

    @BeforeEach
    void setUp() {
        when(repositorioFactory.crearSolicitudRepositorio()).thenReturn(solicitudRepositorio);
        servicioPagos = new ServicioPagos(
                pagoRepositorio,
                metodoPagoRepositorio,
                repositorioFactory,
                pasarelaPagos,
                new TarifaConfig(),
                consultaReportesAbiertos,
                servicioNotificaciones,
                servicioAuditoria
        );
    }

    @Test
    @DisplayName("PU-02: Solicitud EN_DISPUTA con precioFinal 85.000 bloquea custodia y no invoca liberarFondos()")
    void solicitudEnDisputaNoLiberaFondos() {
        Cliente cliente = mock(Cliente.class);
        when(cliente.getIdUsuario()).thenReturn(10L);
        Solicitud solicitud = crearSolicitudBase(cliente, new BigDecimal("85000.00"));
        solicitud.aceptar();
        solicitud.registrarPrecioFinal(new BigDecimal("85000.00"));
        solicitud.rechazarPrecioFinal();

        assertThat(solicitud.getEstadoSolicitud()).isEqualTo(EstadoSolicitud.EN_DISPUTA);
        assertThat(solicitud.puedeLiberarCustodia()).isFalse();

        when(solicitudRepositorio.buscarDetallePorId(1L)).thenReturn(Optional.of(solicitud));
        when(consultaReportesAbiertos.existeReporteAbierto(1L)).thenReturn(false);

        assertThatThrownBy(() -> servicioPagos.liberarCustodiaPorCliente(1L, 10L))
                .isInstanceOf(ExcepcionNegocio.class)
                .hasMessageContaining("EN_DISPUTA");

        verify(pasarelaPagos, never()).liberarFondos(any(), any());
    }

    @Test
    @DisplayName("HU14: Reporte abierto impide liberar custodia aunque la solicitud esté PAGADA")
    void reporteAbiertoImpideLiberarCustodia() {
        Cliente cliente = mock(Cliente.class);
        when(cliente.getIdUsuario()).thenReturn(10L);
        Solicitud solicitud = crearSolicitudBase(cliente, new BigDecimal("70000.00"));
        solicitud.aceptar();
        solicitud.registrarPrecioFinal(new BigDecimal("70000.00"));
        solicitud.confirmarPrecioFinal();
        solicitud.marcarPagada();

        assertThat(solicitud.puedeLiberarCustodia()).isTrue();
        assertThat(solicitud.puedeLiberarCustodia(true)).isFalse();

        when(solicitudRepositorio.buscarDetallePorId(2L)).thenReturn(Optional.of(solicitud));
        when(consultaReportesAbiertos.existeReporteAbierto(2L)).thenReturn(true);

        assertThatThrownBy(() -> servicioPagos.liberarCustodiaPorCliente(2L, 10L))
                .isInstanceOf(ExcepcionNegocio.class)
                .hasMessageContaining("reporte o disputa abierta");

        verify(pasarelaPagos, never()).liberarFondos(any(), any());
    }

    @Test
    @DisplayName("HU13/HU14: PasarelaSimulada valida tarjetas de prueba y Pago genera comprobante sin datos sensibles")
    void pasarelaSimuladaYComprobantePago() {
        PasarelaSimulada simulada = new PasarelaSimulada();
        YearMonth futuro = YearMonth.now().plusYears(2);

        ResultadoValidacionMetodo rechazadaRegistro = simulada.validarMetodo(new DatosTarjetaValidacion(
                "4111111111110001", "123", futuro.getMonthValue(), futuro.getYear(),
                TipoMetodoPago.TARJETA_CREDITO, "Carlos Ramírez"
        ));
        assertThat(rechazadaRegistro.valido()).isFalse();

        YearMonth vencida = YearMonth.now().minusMonths(1);
        ResultadoValidacionMetodo tarjetaVencida = simulada.validarMetodo(new DatosTarjetaValidacion(
                "4242424242424242", "123", vencida.getMonthValue(), vencida.getYear(),
                TipoMetodoPago.TARJETA_CREDITO, "Carlos Ramírez"
        ));
        assertThat(tarjetaVencida.valido()).isFalse();

        ResultadoValidacionMetodo sinFondosReg = simulada.validarMetodo(new DatosTarjetaValidacion(
                "5555555555550002", "321", futuro.getMonthValue(), futuro.getYear(),
                TipoMetodoPago.TARJETA_DEBITO, "Carlos Ramírez"
        ));
        assertThat(sinFondosReg.valido()).isTrue();
        ResultadoTransaccionPasarela cobroSinFondos = simulada.procesarCobro(
                sinFondosReg.token(), new BigDecimal("63000.00"), "PAY-SOL-99");
        assertThat(cobroSinFondos.aprobado()).isFalse();
        assertThat(cobroSinFondos.motivoRechazo()).contains("Fondos insuficientes");

        DatosTarjetaValidacion datosValidos = new DatosTarjetaValidacion(
                "4242424242424242", "999", futuro.getMonthValue(), futuro.getYear(),
                TipoMetodoPago.TARJETA_CREDITO, "Carlos Ramírez"
        );
        assertThat(datosValidos.toString()).doesNotContain("424242424242").doesNotContain("999");

        ResultadoValidacionMetodo validacionOk = simulada.validarMetodo(datosValidos);
        assertThat(validacionOk.valido()).isTrue();

        Cliente cliente = mock(Cliente.class);
        when(cliente.getNombre()).thenReturn("Carlos Ramírez");
        Profesional pro = mock(Profesional.class);
        when(pro.getNombre()).thenReturn("Juan Pérez");
        Servicio srv = new Servicio(pro, new CategoriaOficio("Electricidad", "Desc"));
        Solicitud solicitud = new Solicitud(cliente, pro, srv, "Arreglo tablero", List.of(), false, new BigDecimal("60000"));
        MetodoPago metodo = new MetodoPago(
                cliente, validacionOk.token(), TipoMetodoPago.TARJETA_CREDITO,
                validacionOk.franquicia(), validacionOk.ultimosDigitos(),
                "Carlos Ramírez", futuro.getMonthValue(), futuro.getYear()
        );

        Pago pago = new Pago(solicitud, metodo, new BigDecimal("60000.00"), new BigDecimal("3000.00"), "PAY-SOL-1");
        pago.ejecutarCobro(simulada);

        assertThat(pago.getEstadoPago()).isEqualTo(EstadoPago.RETENIDO);
        assertThat(pago.getCantidad()).isEqualByComparingTo("63000.00");

        ComprobantePagoDTO comprobante = pago.generarComprobante();
        assertThat(comprobante).isNotNull();
        assertThat(comprobante.montoServicio()).isEqualByComparingTo("60000.00");
        assertThat(comprobante.tasaGarantia()).isEqualByComparingTo("3000.00");
        assertThat(comprobante.totalPagado()).isEqualByComparingTo("63000.00");
        assertThat(comprobante.metodoPagoResumen()).isEqualTo("VISA •••• 4242");
    }

    private Solicitud crearSolicitudBase(Cliente cliente, BigDecimal monto) {
        return new Solicitud(
                cliente,
                mock(Profesional.class),
                mock(Servicio.class),
                "Servicio de prueba",
                List.of(),
                false,
                monto
        );
    }
}
