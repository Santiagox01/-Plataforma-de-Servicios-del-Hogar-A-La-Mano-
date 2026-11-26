package co.edu.udea.alamano.pagos;

import co.edu.udea.alamano.comun.configuracion.PropiedadesApp;
import co.edu.udea.alamano.comun.configuracion.TarifaConfig;
import co.edu.udea.alamano.pagos.dominio.DatosTarjetaValidacion;
import co.edu.udea.alamano.pagos.dominio.ResultadoAutorizacionPago;
import co.edu.udea.alamano.pagos.dominio.ResultadoInicioPago;
import co.edu.udea.alamano.pagos.dominio.ResultadoTransaccionPasarela;
import co.edu.udea.alamano.pagos.dominio.ResultadoValidacionMetodo;
import co.edu.udea.alamano.pagos.dominio.TipoMetodoPago;
import co.edu.udea.alamano.pagos.infraestructura.PayPalAdapter;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class PayPalAdapterTest {

    private static final String CLIENT_ID = "cli-sandbox-publico";
    private static final String CLIENT_SECRET = "sec-sandbox-super-privado";
    private static final String ACCESS_TOKEN = "tok-oauth-privado-999";

    private MockWebServer mockWebServer;
    private ObjectMapper objectMapper;
    private PayPalAdapter payPalAdapter;
    private TarifaConfig tarifaConfig;

    @BeforeEach
    void setUp() throws Exception {
        mockWebServer = new MockWebServer();
        mockWebServer.start();
        objectMapper = new ObjectMapper();
        tarifaConfig = new TarifaConfig();
        tarifaConfig.setTasaCambioCopUsd(new BigDecimal("4000.00"));

        PropiedadesApp props = new PropiedadesApp();
        props.getPagos().setPasarela("paypal");
        props.getPagos().getPaypal().setClientId(CLIENT_ID);
        props.getPagos().getPaypal().setClientSecret(CLIENT_SECRET);
        props.getPagos().getPaypal().setBaseUrl(mockWebServer.url("/").toString());
        props.getPagos().getPaypal().setReturnUrl("http://localhost:8080/api/pagos/paypal/retorno");
        props.getPagos().getPaypal().setCancelUrl("http://localhost:8080/api/pagos/paypal/cancelado");

        assertThat(props.getPagos().getPaypal().toString()).doesNotContain(CLIENT_SECRET);
        payPalAdapter = new PayPalAdapter(props, objectMapper);
    }

    @AfterEach
    void tearDown() throws Exception {
        mockWebServer.shutdown();
    }

    @Test
    @DisplayName("PayPal: convierte COP->USD, cachea OAuth2 token, crea orden AUTHORIZE y autoriza tras validar APPROVED")
    void creaOrdenAutorizaYCacheaToken() throws Exception {
        // 1. Respuesta OAuth2 (solo debe invocarse 1 vez para las operaciones consecutivas)
        mockWebServer.enqueue(jsonResponse(200, """
                {"access_token": "%s", "token_type": "Bearer", "expires_in": 32400}
                """.formatted(ACCESS_TOKEN)));

        // 2. Respuesta POST /v2/checkout/orders
        mockWebServer.enqueue(jsonResponse(201, """
                {
                  "id": "ORD-PAYPAL-100",
                  "status": "PAYER_ACTION_REQUIRED",
                  "links": [
                    {"href": "https://www.sandbox.paypal.com/checkoutnow?token=ORD-PAYPAL-100", "rel": "payer-action"}
                  ]
                }
                """));

        // 3. Respuesta GET /v2/checkout/orders/ORD-PAYPAL-100
        mockWebServer.enqueue(jsonResponse(200, """
                {"id": "ORD-PAYPAL-100", "status": "APPROVED"}
                """));

        // 4. Respuesta POST /v2/checkout/orders/ORD-PAYPAL-100/authorize
        mockWebServer.enqueue(jsonResponse(201, """
                {
                  "id": "ORD-PAYPAL-100",
                  "status": "COMPLETED",
                  "purchase_units": [
                    {
                      "payments": {
                        "authorizations": [
                          {
                            "id": "AUTH-PP-777",
                            "status": "CREATED",
                            "expiration_time": "2026-12-25T10:00:00Z"
                          }
                        ]
                      }
                    }
                  ]
                }
                """));

        BigDecimal montoUsd = tarifaConfig.convertirCopAUsd(new BigDecimal("63000.00"));
        assertThat(montoUsd).isEqualByComparingTo("15.75");

        ResultadoInicioPago inicio = payPalAdapter.iniciarPago(
                montoUsd, "USD", "PAY-SOL-10", "Electricidad",
                "http://localhost:8080/api/pagos/paypal/retorno?appRedirect=alamano://pago",
                "http://localhost:8080/api/pagos/paypal/cancelado"
        );
        assertThat(inicio.creado()).isTrue();
        assertThat(inicio.idOrdenExterna()).isEqualTo("ORD-PAYPAL-100");
        assertThat(inicio.urlAprobacion()).contains("ORD-PAYPAL-100");

        ResultadoAutorizacionPago auth = payPalAdapter.confirmarYAutorizarPago("ORD-PAYPAL-100", "PAY-SOL-10");
        assertThat(auth.aprobado()).isTrue();
        assertThat(auth.idAutorizacion()).isEqualTo("AUTH-PP-777");
        assertThat(auth.expiraEn()).isEqualTo(OffsetDateTime.parse("2026-12-25T10:00:00Z"));

        // Solo 4 peticiones HTTP: 1 token + 1 create order + 1 get order + 1 authorize (token cacheado)
        assertThat(mockWebServer.getRequestCount()).isEqualTo(4);

        RecordedRequest reqToken = mockWebServer.takeRequest();
        assertThat(reqToken.getPath()).isEqualTo("/v1/oauth2/token");
        assertThat(reqToken.getHeader("Authorization")).startsWith("Basic ");

        RecordedRequest reqCreate = mockWebServer.takeRequest();
        assertThat(reqCreate.getPath()).isEqualTo("/v2/checkout/orders");
        assertThat(reqCreate.getHeader("Authorization")).isEqualTo("Bearer " + ACCESS_TOKEN);
        assertThat(reqCreate.getHeader("PayPal-Request-Id")).isEqualTo("ORD-PAY-SOL-10");
        JsonNode bodyCreate = objectMapper.readTree(reqCreate.getBody().readUtf8());
        assertThat(bodyCreate.path("intent").asText()).isEqualTo("AUTHORIZE");
        assertThat(bodyCreate.path("purchase_units").get(0).path("amount").path("currency_code").asText()).isEqualTo("USD");
        assertThat(bodyCreate.path("purchase_units").get(0).path("amount").path("value").asText()).isEqualTo("15.75");

        RecordedRequest reqGet = mockWebServer.takeRequest();
        assertThat(reqGet.getMethod()).isEqualTo("GET");
        assertThat(reqGet.getPath()).isEqualTo("/v2/checkout/orders/ORD-PAYPAL-100");

        RecordedRequest reqAuth = mockWebServer.takeRequest();
        assertThat(reqAuth.getPath()).isEqualTo("/v2/checkout/orders/ORD-PAYPAL-100/authorize");
        assertThat(reqAuth.getHeader("PayPal-Request-Id")).isEqualTo("AUTH-PAY-SOL-10");
    }

    @Test
    @DisplayName("PayPal: captura autorización (LIBERADO), anula autorización (void) y reembolsa captura (refund)")
    void capturaAnulacionYReembolso() throws Exception {
        mockWebServer.enqueue(jsonResponse(200, """
                {"access_token": "%s", "expires_in": 32400}
                """.formatted(ACCESS_TOKEN)));
        mockWebServer.enqueue(jsonResponse(201, """
                {"id": "CAP-PP-555", "status": "COMPLETED"}
                """));
        mockWebServer.enqueue(new MockResponse().setResponseCode(204));
        mockWebServer.enqueue(jsonResponse(201, """
                {"id": "REF-PP-333", "status": "COMPLETED"}
                """));

        ResultadoTransaccionPasarela captura = payPalAdapter.liberarFondos("AUTH-PP-777", new BigDecimal("15.75"));
        assertThat(captura.aprobado()).isTrue();
        assertThat(captura.referenciaPasarela()).isEqualTo("CAP-PP-555");

        ResultadoTransaccionPasarela anulacion = payPalAdapter.reembolsarFondos("AUTH-PP-777", new BigDecimal("15.75"), "Cancelado en disputa");
        assertThat(anulacion.aprobado()).isTrue();

        ResultadoTransaccionPasarela reembolsoCap = payPalAdapter.reembolsarCaptura("CAP-PP-555", new BigDecimal("15.75"), "Reembolso resuelto por admin");
        assertThat(reembolsoCap.aprobado()).isTrue();
        assertThat(reembolsoCap.referenciaPasarela()).isEqualTo("REF-PP-333");

        mockWebServer.takeRequest(); // token
        assertThat(mockWebServer.takeRequest().getPath()).isEqualTo("/v2/payments/authorizations/AUTH-PP-777/capture");
        assertThat(mockWebServer.takeRequest().getPath()).isEqualTo("/v2/payments/authorizations/AUTH-PP-777/void");
        assertThat(mockWebServer.takeRequest().getPath()).isEqualTo("/v2/payments/captures/CAP-PP-555/refund");
    }

    @Test
    @DisplayName("PayPal: rechaza orden sin aprobar, maneja AUTHORIZATION_EXPIRED y no filtra secretos ni tokens")
    void ordenNoAprobadaYErroresSeguros() {
        mockWebServer.enqueue(jsonResponse(200, """
                {"access_token": "%s", "expires_in": 32400}
                """.formatted(ACCESS_TOKEN)));
        mockWebServer.enqueue(jsonResponse(200, """
                {"id": "ORD-SIN-APROBAR", "status": "PAYER_ACTION_REQUIRED"}
                """));
        mockWebServer.enqueue(jsonResponse(422, """
                {
                  "name": "UNPROCESSABLE_ENTITY",
                  "message": "The requested action could not be performed.",
                  "details": [
                    {
                      "issue": "AUTHORIZATION_EXPIRED",
                      "description": "The authorization has expired."
                    }
                  ]
                }
                """));

        ResultadoAutorizacionPago noAprobada = payPalAdapter.confirmarYAutorizarPago("ORD-SIN-APROBAR", "PAY-SOL-11");
        assertThat(noAprobada.aprobado()).isFalse();
        assertThat(noAprobada.motivoRechazo()).contains("PAYER_ACTION_REQUIRED");

        ResultadoTransaccionPasarela expirada = payPalAdapter.liberarFondos("AUTH-VENCIDA", new BigDecimal("10.00"));
        assertThat(expirada.aprobado()).isFalse();
        assertThat(expirada.motivoRechazo())
                .contains("expiró")
                .doesNotContain(CLIENT_SECRET)
                .doesNotContain(ACCESS_TOKEN);

        ResultadoValidacionMetodo valTarjeta = payPalAdapter.validarMetodo(
                new DatosTarjetaValidacion("4242424242424242", "123", 12, 2028, TipoMetodoPago.TARJETA_CREDITO, "Carlos")
        );
        assertThat(valTarjeta.valido()).isFalse();
    }

    private MockResponse jsonResponse(int codigo, String json) {
        return new MockResponse()
                .setResponseCode(codigo)
                .addHeader("Content-Type", "application/json")
                .setBody(json);
    }
}
