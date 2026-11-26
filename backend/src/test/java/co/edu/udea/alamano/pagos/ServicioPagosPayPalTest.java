package co.edu.udea.alamano.pagos;

import co.edu.udea.alamano.autenticacion_seguridad.dominio.Cliente;
import co.edu.udea.alamano.autenticacion_seguridad.dominio.Profesional;
import co.edu.udea.alamano.autenticacion_seguridad.infraestructura.RepositorioFactory;
import co.edu.udea.alamano.comun.auditoria.ServicioAuditoria;
import co.edu.udea.alamano.comun.configuracion.PropiedadesApp;
import co.edu.udea.alamano.comun.configuracion.TarifaConfig;
import co.edu.udea.alamano.comun.notificacion.ServicioNotificaciones;
import co.edu.udea.alamano.cotizacion_tarificacion.dominio.CategoriaOficio;
import co.edu.udea.alamano.cotizacion_tarificacion.dominio.Servicio;
import co.edu.udea.alamano.gestion_servicios.aplicacion.IConsultaReportesAbiertos;
import co.edu.udea.alamano.gestion_servicios.dominio.EstadoSolicitud;
import co.edu.udea.alamano.gestion_servicios.dominio.Solicitud;
import co.edu.udea.alamano.gestion_servicios.infraestructura.ISolicitudRepositorio;
import co.edu.udea.alamano.pagos.aplicacion.ServicioPagos;
import co.edu.udea.alamano.pagos.dominio.EstadoPago;
import co.edu.udea.alamano.pagos.dominio.MetodoPago;
import co.edu.udea.alamano.pagos.dominio.Pago;
import co.edu.udea.alamano.pagos.dominio.TipoMetodoPago;
import co.edu.udea.alamano.pagos.dto.PagoDTO;
import co.edu.udea.alamano.pagos.dto.RespuestaInicioPagoDTO;
import co.edu.udea.alamano.pagos.infraestructura.MetodoPagoRepositorio;
import co.edu.udea.alamano.pagos.infraestructura.PagoRepositorio;
import co.edu.udea.alamano.pagos.infraestructura.PayPalAdapter;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ServicioPagosPayPalTest {

    private MockWebServer mockWebServer;

    @Mock
    private PagoRepositorio pagoRepositorio;

    @Mock
    private MetodoPagoRepositorio metodoPagoRepositorio;

    @Mock
    private RepositorioFactory repositorioFactory;

    @Mock
    private ISolicitudRepositorio solicitudRepositorio;

    @Mock
    private IConsultaReportesAbiertos consultaReportesAbiertos;

    @Mock
    private ServicioNotificaciones servicioNotificaciones;

    @Mock
    private ServicioAuditoria servicioAuditoria;

    private ServicioPagos servicioPagos;
    private Solicitud solicitudFinalizada;
    private MetodoPago metodoPayPal;

    @BeforeEach
    void setUp() throws Exception {
        mockWebServer = new MockWebServer();
        mockWebServer.start();

        PropiedadesApp props = new PropiedadesApp();
        props.getPagos().setPasarela("paypal");
        props.getPagos().getPaypal().setClientId("cli-test");
        props.getPagos().getPaypal().setClientSecret("sec-test");
        props.getPagos().getPaypal().setBaseUrl(mockWebServer.url("/").toString());
        props.getPagos().getPaypal().setReturnUrl("http://localhost:8080/api/pagos/paypal/retorno");
        props.getPagos().getPaypal().setCancelUrl("http://localhost:8080/api/pagos/paypal/cancelado");

        TarifaConfig tarifaConfig = new TarifaConfig();
        tarifaConfig.setTasaCambioCopUsd(new BigDecimal("4000.00"));

        when(repositorioFactory.crearSolicitudRepositorio()).thenReturn(solicitudRepositorio);
        PayPalAdapter payPalAdapter = new PayPalAdapter(props, new ObjectMapper());

        servicioPagos = new ServicioPagos(
                pagoRepositorio,
                metodoPagoRepositorio,
                repositorioFactory,
                payPalAdapter,
                tarifaConfig,
                consultaReportesAbiertos,
                servicioNotificaciones,
                servicioAuditoria,
                props
        );

        Cliente cliente = mock(Cliente.class);
        when(cliente.getIdUsuario()).thenReturn(1L);
        when(cliente.getNombre()).thenReturn("Carlos Ramírez");

        Profesional pro = mock(Profesional.class);
        when(pro.getIdUsuario()).thenReturn(2L);
        when(pro.getNombre()).thenReturn("Juan Pérez");

        Servicio srv = new Servicio(pro, new CategoriaOficio("Electricidad", "Desc"));
        solicitudFinalizada = new Solicitud(cliente, pro, srv, "Arreglo tablero", List.of(), false, new BigDecimal("50000"));
        asignarId(solicitudFinalizada, "idSolicitud", 77L);
        solicitudFinalizada.aceptar();
        solicitudFinalizada.registrarPrecioFinal(new BigDecimal("60000.00"));
        solicitudFinalizada.confirmarPrecioFinal();

        metodoPayPal = MetodoPago.crearMetodoPayPal(cliente, "carlos@alamano.co");
    }

    @AfterEach
    void tearDown() throws Exception {
        mockWebServer.shutdown();
    }

    @Test
    @DisplayName("Flujo completo PayPal Sandbox: iniciar -> confirmar (RETENIDO) -> liberar (LIBERADO) -> reembolsar captura")
    void flujoCompletoPayPalConIdempotenciaYConversionUsd() throws Exception {
        mockWebServer.enqueue(jsonResponse(200, """
                {"access_token": "tok-123", "expires_in": 32400}
                """));
        mockWebServer.enqueue(jsonResponse(201, """
                {
                  "id": "ORD-PP-77",
                  "status": "PAYER_ACTION_REQUIRED",
                  "links": [{"href": "https://www.sandbox.paypal.com/checkoutnow?token=ORD-PP-77", "rel": "payer-action"}]
                }
                """));
        mockWebServer.enqueue(jsonResponse(200, """
                {"id": "ORD-PP-77", "status": "APPROVED"}
                """));
        mockWebServer.enqueue(jsonResponse(201, """
                {
                  "id": "ORD-PP-77",
                  "status": "COMPLETED",
                  "purchase_units": [{"payments": {"authorizations": [{"id": "AUTH-PP-77", "status": "CREATED", "expiration_time": "2026-12-26T10:00:00Z"}]}}]
                }
                """));
        mockWebServer.enqueue(jsonResponse(201, """
                {"id": "CAP-PP-77", "status": "COMPLETED"}
                """));
        mockWebServer.enqueue(jsonResponse(201, """
                {"id": "REF-PP-77", "status": "COMPLETED"}
                """));

        AtomicReference<Pago> pagoAlmacenado = new AtomicReference<>();

        when(solicitudRepositorio.buscarDetallePorId(77L)).thenReturn(Optional.of(solicitudFinalizada));
        when(solicitudRepositorio.guardar(any(Solicitud.class))).thenAnswer(inv -> inv.getArgument(0));
        when(metodoPagoRepositorio.findFirstByClienteIdUsuarioAndTipoAndActivoTrue(1L, TipoMetodoPago.PAYPAL))
                .thenReturn(Optional.of(metodoPayPal));
        when(pagoRepositorio.saveAndFlush(any(Pago.class))).thenAnswer(inv -> {
            Pago p = inv.getArgument(0);
            asignarId(p, "idPago", 500L);
            pagoAlmacenado.set(p);
            return p;
        });
        when(pagoRepositorio.save(any(Pago.class))).thenAnswer(inv -> {
            Pago p = inv.getArgument(0);
            pagoAlmacenado.set(p);
            return p;
        });
        when(pagoRepositorio.buscarPagoActivoPorSolicitud(eq(77L), anyList())).thenAnswer(inv -> {
            Pago p = pagoAlmacenado.get();
            if (p != null && (p.getEstadoPago() == EstadoPago.RETENIDO
                    || p.getEstadoPago() == EstadoPago.LIBERADO
                    || p.getEstadoPago() == EstadoPago.REEMBOLSADO)) {
                return Optional.of(p);
            }
            return Optional.empty();
        });
        when(pagoRepositorio.buscarPendienteAprobacionPorSolicitud(77L)).thenAnswer(inv -> {
            Pago p = pagoAlmacenado.get();
            if (p != null && p.getEstadoPago() == EstadoPago.PENDIENTE_APROBACION) {
                return Optional.of(p);
            }
            return Optional.empty();
        });
        when(consultaReportesAbiertos.existeReporteAbierto(77L)).thenReturn(false);

        // 1. Iniciar pago externo: 60.000 + 3.000 = 63.000 COP = 15.75 USD
        RespuestaInicioPagoDTO inicio = servicioPagos.iniciarPagoExterno(77L, 1L, "alamano://pago");
        assertThat(inicio.estadoPago()).isEqualTo(EstadoPago.PENDIENTE_APROBACION);
        assertThat(inicio.idOrdenExterna()).isEqualTo("ORD-PP-77");
        assertThat(inicio.totalCop()).isEqualByComparingTo("63000.00");
        assertThat(inicio.montoUsd()).isEqualByComparingTo("15.75");
        assertThat(inicio.idempotente()).isFalse();

        // Segundo iniciar mientras sigue PENDIENTE -> devuelve el mismo pago idempotentemente
        RespuestaInicioPagoDTO inicioRepetido = servicioPagos.iniciarPagoExterno(77L, 1L, "alamano://pago");
        assertThat(inicioRepetido.idempotente()).isTrue();
        assertThat(inicioRepetido.idOrdenExterna()).isEqualTo("ORD-PP-77");

        // 2. Confirmar tras aprobacion en PayPal -> RETENIDO + Solicitud PAGADA + Comprobante COP/USD
        PagoDTO confirmado = servicioPagos.confirmarPagoExterno(77L, 1L, "ORD-PP-77");
        assertThat(confirmado.estadoPago()).isEqualTo(EstadoPago.RETENIDO);
        assertThat(confirmado.pasarela()).isEqualTo("paypal");
        assertThat(confirmado.montoUsd()).isEqualByComparingTo("15.75");
        assertThat(confirmado.comprobante()).isNotNull();
        assertThat(confirmado.comprobante().montoUsd()).isEqualByComparingTo("15.75");
        assertThat(solicitudFinalizada.getEstadoSolicitud()).isEqualTo(EstadoSolicitud.PAGADA);

        // Doble confirmacion -> idempotente sin llamar de nuevo a PayPal
        PagoDTO confirmadoDuplicado = servicioPagos.confirmarPagoExterno(77L, 1L, "ORD-PP-77");
        assertThat(confirmadoDuplicado.idempotente()).isTrue();
        assertThat(confirmadoDuplicado.idPago()).isEqualTo(500L);

        // 3. Liberar custodia al confirmar servicio recibido -> captura en PayPal -> LIBERADO
        PagoDTO liberado = servicioPagos.liberarCustodiaPorCliente(77L, 1L);
        assertThat(liberado.estadoPago()).isEqualTo(EstadoPago.LIBERADO);
        assertThat(pagoAlmacenado.get().getReferenciaCaptura()).isEqualTo("CAP-PP-77");

        // 4. Reembolsar captura por moderacion -> REEMBOLSADO
        PagoDTO reembolsado = servicioPagos.reembolsarPorModeracion(77L, 99L, "Inconformidad comprobada");
        assertThat(reembolsado.estadoPago()).isEqualTo(EstadoPago.REEMBOLSADO);
    }

    @Test
    @DisplayName("PayPal: cancelación antes de aprobar y anulación de autorización antes de capturar")
    void cancelacionYVoidAntesDeCapturar() throws Exception {
        mockWebServer.enqueue(jsonResponse(200, """
                {"access_token": "tok-123", "expires_in": 32400}
                """));
        mockWebServer.enqueue(jsonResponse(201, """
                {
                  "id": "ORD-CANC-1",
                  "status": "PAYER_ACTION_REQUIRED",
                  "links": [{"href": "https://www.sandbox.paypal.com/checkoutnow?token=ORD-CANC-1", "rel": "approve"}]
                }
                """));
        mockWebServer.enqueue(jsonResponse(201, """
                {
                  "id": "ORD-VOID-2",
                  "status": "PAYER_ACTION_REQUIRED",
                  "links": [{"href": "https://www.sandbox.paypal.com/checkoutnow?token=ORD-VOID-2", "rel": "approve"}]
                }
                """));
        mockWebServer.enqueue(jsonResponse(200, """
                {"id": "ORD-VOID-2", "status": "APPROVED"}
                """));
        mockWebServer.enqueue(jsonResponse(201, """
                {
                  "id": "ORD-VOID-2",
                  "status": "COMPLETED",
                  "purchase_units": [{"payments": {"authorizations": [{"id": "AUTH-VOID-2", "status": "CREATED"}]}}]
                }
                """));
        mockWebServer.enqueue(new MockResponse().setResponseCode(204));

        AtomicReference<Pago> pagoAlmacenado = new AtomicReference<>();

        when(solicitudRepositorio.buscarDetallePorId(77L)).thenReturn(Optional.of(solicitudFinalizada));
        when(solicitudRepositorio.guardar(any(Solicitud.class))).thenAnswer(inv -> inv.getArgument(0));
        when(metodoPagoRepositorio.findFirstByClienteIdUsuarioAndTipoAndActivoTrue(1L, TipoMetodoPago.PAYPAL))
                .thenReturn(Optional.of(metodoPayPal));
        when(pagoRepositorio.saveAndFlush(any(Pago.class))).thenAnswer(inv -> {
            Pago p = inv.getArgument(0);
            asignarId(p, "idPago", 501L);
            pagoAlmacenado.set(p);
            return p;
        });
        when(pagoRepositorio.save(any(Pago.class))).thenAnswer(inv -> {
            Pago p = inv.getArgument(0);
            pagoAlmacenado.set(p);
            return p;
        });
        when(pagoRepositorio.buscarPagoActivoPorSolicitud(eq(77L), anyList())).thenAnswer(inv -> {
            Pago p = pagoAlmacenado.get();
            if (p != null && p.getEstadoPago() == EstadoPago.RETENIDO) {
                return Optional.of(p);
            }
            return Optional.empty();
        });
        when(pagoRepositorio.buscarPendienteAprobacionPorSolicitud(77L)).thenAnswer(inv -> {
            Pago p = pagoAlmacenado.get();
            if (p != null && p.getEstadoPago() == EstadoPago.PENDIENTE_APROBACION) {
                return Optional.of(p);
            }
            return Optional.empty();
        });

        // 1. Inicia y el usuario cancela -> CANCELADO y la solicitud sigue FINALIZADA
        servicioPagos.iniciarPagoExterno(77L, 1L, null);
        PagoDTO cancelado = servicioPagos.cancelarPagoPendiente(77L, 1L);
        assertThat(cancelado.estadoPago()).isEqualTo(EstadoPago.CANCELADO);
        assertThat(solicitudFinalizada.getEstadoSolicitud()).isEqualTo(EstadoSolicitud.FINALIZADA);

        // 2. Nuevo intento -> confirma (RETENIDO) -> moderacion anula autorizacion (void 204) -> REEMBOLSADO
        servicioPagos.iniciarPagoExterno(77L, 1L, null);
        PagoDTO retenido = servicioPagos.confirmarPagoExterno(77L, 1L, "ORD-VOID-2");
        assertThat(retenido.estadoPago()).isEqualTo(EstadoPago.RETENIDO);

        PagoDTO anulado = servicioPagos.reembolsarPorModeracion(77L, 99L, "Sin acuerdo");
        assertThat(anulado.estadoPago()).isEqualTo(EstadoPago.REEMBOLSADO);
    }

    private MockResponse jsonResponse(int codigo, String json) {
        return new MockResponse()
                .setResponseCode(codigo)
                .addHeader("Content-Type", "application/json")
                .setBody(json);
    }

    private void asignarId(Object objeto, String campo, Long valor) throws Exception {
        Field f = objeto.getClass().getDeclaredField(campo);
        f.setAccessible(true);
        f.set(objeto, valor);
    }
}
