package co.edu.udea.alamano.pagos.infraestructura;

import co.edu.udea.alamano.comun.configuracion.PropiedadesApp;
import co.edu.udea.alamano.comun.excepcion.ExcepcionNegocio;
import co.edu.udea.alamano.pagos.dominio.DatosTarjetaValidacion;
import co.edu.udea.alamano.pagos.dominio.FranquiciaTarjeta;
import co.edu.udea.alamano.pagos.dominio.IPasarelaPagoRedireccion;
import co.edu.udea.alamano.pagos.dominio.ResultadoAutorizacionPago;
import co.edu.udea.alamano.pagos.dominio.ResultadoInicioPago;
import co.edu.udea.alamano.pagos.dominio.ResultadoTransaccionPasarela;
import co.edu.udea.alamano.pagos.dominio.ResultadoValidacionMetodo;
import co.edu.udea.alamano.pagos.dominio.TipoMetodoPago;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * Adaptador de pasarela real PayPal Sandbox usando REST API v2 (Orders y Payments) sin SDK externo.
 * Implementa el esquema escrow de dos pasos:
 *  - Crear orden con intent=AUTHORIZE y redirigir a aprobacion
 *  - Confirmar estado en PayPal y autorizar (RETENIDO)
 *  - Capturar autorizacion al confirmar servicio recibido (LIBERADO)
 *  - Anular autorizacion (void) o reembolsar captura (refund) segun corresponda (REEMBOLSADO)
 */
@Component
@ConditionalOnProperty(name = "app.pagos.pasarela", havingValue = "paypal")
public class PayPalAdapter implements IPasarelaPagoRedireccion {

    private static final String TIPO_PASARELA = "paypal";
    private static final String MONEDA_USD = "USD";
    private static final String INTENT_AUTHORIZE = "AUTHORIZE";
    private static final String ESTADO_ORDEN_APROBADA = "APPROVED";
    private static final String ESTADO_ORDEN_COMPLETADA = "COMPLETED";
    private static final String ESTADO_AUTORIZACION_DENEGADA = "DENIED";
    private static final String ESTADO_AUTORIZACION_ANULADA = "VOIDED";
    private static final String ISSUE_AUTORIZACION_EXPIRADA = "AUTHORIZATION_EXPIRED";
    private static final long MARGEN_EXPIRACION_TOKEN_SEGUNDOS = 60L;
    private static final int MAX_DESCRIPCION = 120;
    private static final int MAX_NOTA_REEMBOLSO = 250;

    private final PropiedadesApp propiedades;
    private final ObjectMapper objectMapper;
    private final RestClient restClient;

    private String tokenAccesoCache;
    private Instant expiracionTokenCache = Instant.EPOCH;

    public PayPalAdapter(PropiedadesApp propiedades, ObjectMapper objectMapper) {
        this.propiedades = propiedades;
        this.objectMapper = objectMapper;
        String baseUrl = propiedades.getPagos().getPaypal().getBaseUrl();
        this.restClient = RestClient.builder()
                .baseUrl((baseUrl == null ? "https://api-m.sandbox.paypal.com" : baseUrl).replaceAll("/+$", ""))
                .build();
    }

    @Override
    public String obtenerTipoPasarela() {
        return TIPO_PASARELA;
    }

    @Override
    public ResultadoValidacionMetodo validarMetodo(DatosTarjetaValidacion datos) {
        if (datos != null && datos.tipo() == TipoMetodoPago.PAYPAL) {
            String token = "paypal_tmp_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
            return new ResultadoValidacionMetodo(true, token, FranquiciaTarjeta.PAYPAL, "PPAL", null);
        }
        return new ResultadoValidacionMetodo(
                false,
                null,
                null,
                null,
                "Con PayPal activo no se registran tarjetas directamente; el pago se aprueba en PayPal Sandbox."
        );
    }

    @Override
    public ResultadoInicioPago iniciarPago(BigDecimal montoExterno,
                                           String moneda,
                                           String referenciaIdempotencia,
                                           String descripcion,
                                           String urlRetornoPersonalizada,
                                           String urlCancelacionPersonalizada) {
        if (montoExterno == null || montoExterno.compareTo(BigDecimal.ZERO) <= 0) {
            return new ResultadoInicioPago(false, null, null, "El monto convertido en USD debe ser mayor a cero.");
        }
        try {
            String token = obtenerAccessToken();
            PropiedadesApp.Paypal cfg = propiedades.getPagos().getPaypal();
            String returnUrl = (urlRetornoPersonalizada != null && !urlRetornoPersonalizada.isBlank())
                    ? urlRetornoPersonalizada
                    : cfg.getReturnUrl();
            String cancelUrl = (urlCancelacionPersonalizada != null && !urlCancelacionPersonalizada.isBlank())
                    ? urlCancelacionPersonalizada
                    : cfg.getCancelUrl();

            Map<String, Object> experiencia = new LinkedHashMap<>();
            experiencia.put("brand_name", "A La Mano");
            experiencia.put("locale", "es-CO");
            experiencia.put("shipping_preference", "NO_SHIPPING");
            experiencia.put("user_action", "PAY_NOW");
            experiencia.put("return_url", returnUrl);
            experiencia.put("cancel_url", cancelUrl);

            Map<String, Object> payload = Map.of(
                    "intent", INTENT_AUTHORIZE,
                    "purchase_units", List.of(Map.of(
                            "reference_id", referenciaIdempotencia,
                            "description", truncar(descripcion, MAX_DESCRIPCION, "Servicio A La Mano"),
                            "amount", Map.of(
                                    "currency_code", moneda != null ? moneda : MONEDA_USD,
                                    "value", formatearMonto(montoExterno)
                            )
                    )),
                    "payment_source", Map.of(
                            "paypal", Map.of("experience_context", experiencia)
                    )
            );

            String respuestaJson = restClient.post()
                    .uri("/v2/checkout/orders")
                    .header("Authorization", "Bearer " + token)
                    .header("PayPal-Request-Id", "ORD-" + referenciaIdempotencia)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(payload)
                    .retrieve()
                    .body(String.class);

            JsonNode nodo = leerJson(respuestaJson);
            String orderId = nodo.path("id").asText(null);
            String urlAprobacion = extraerUrlAprobacion(nodo.path("links"));

            if (orderId == null || orderId.isBlank() || urlAprobacion == null || urlAprobacion.isBlank()) {
                return new ResultadoInicioPago(false, orderId, null,
                        "PayPal no devolvió el identificador de orden o el enlace de aprobación.");
            }
            return new ResultadoInicioPago(true, orderId, urlAprobacion, null);
        } catch (ExcepcionNegocio e) {
            return new ResultadoInicioPago(false, null, null, e.getMessage());
        } catch (RestClientResponseException e) {
            return new ResultadoInicioPago(false, null, null, extraerErrorPayPalSeguro(e, "No se pudo crear la orden en PayPal."));
        } catch (RestClientException e) {
            return new ResultadoInicioPago(false, null, null, "Error de comunicación al conectar con PayPal.");
        }
    }

    @Override
    public ResultadoAutorizacionPago confirmarYAutorizarPago(String idOrdenExterna, String referenciaIdempotencia) {
        if (idOrdenExterna == null || idOrdenExterna.isBlank()) {
            return new ResultadoAutorizacionPago(false, null, null, null, "Falta el identificador de la orden de PayPal.");
        }
        try {
            String token = obtenerAccessToken();
            String ordenJson = restClient.get()
                    .uri("/v2/checkout/orders/{id}", idOrdenExterna)
                    .header("Authorization", "Bearer " + token)
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .body(String.class);

            JsonNode nodoOrden = leerJson(ordenJson);
            String estadoOrden = nodoOrden.path("status").asText("");

            if (ESTADO_ORDEN_COMPLETADA.equalsIgnoreCase(estadoOrden)) {
                ResultadoAutorizacionPago yaAutorizada = extraerAutorizacion(nodoOrden, idOrdenExterna);
                if (yaAutorizada.aprobado()) {
                    return yaAutorizada;
                }
            }

            if (!ESTADO_ORDEN_APROBADA.equalsIgnoreCase(estadoOrden)) {
                return new ResultadoAutorizacionPago(
                        false,
                        idOrdenExterna,
                        null,
                        null,
                        "La orden en PayPal aún no ha sido aprobada por el cliente (estado actual: " + estadoOrden + ")."
                );
            }

            String authJson = restClient.post()
                    .uri("/v2/checkout/orders/{id}/authorize", idOrdenExterna)
                    .header("Authorization", "Bearer " + token)
                    .header("PayPal-Request-Id", "AUTH-" + referenciaIdempotencia)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of())
                    .retrieve()
                    .body(String.class);

            JsonNode nodoAuth = leerJson(authJson);
            return extraerAutorizacion(nodoAuth, idOrdenExterna);
        } catch (ExcepcionNegocio e) {
            return new ResultadoAutorizacionPago(false, idOrdenExterna, null, null, e.getMessage());
        } catch (RestClientResponseException e) {
            return new ResultadoAutorizacionPago(false, idOrdenExterna, null, null,
                    extraerErrorPayPalSeguro(e, "PayPal rechazó la autorización de la orden."));
        } catch (RestClientException e) {
            return new ResultadoAutorizacionPago(false, idOrdenExterna, null, null,
                    "Error de red al confirmar la orden con PayPal.");
        }
    }

    @Override
    public ResultadoTransaccionPasarela procesarCobro(String tokenMetodo,
                                                      BigDecimal montoTotal,
                                                      String referenciaIdempotencia) {
        return new ResultadoTransaccionPasarela(
                false,
                null,
                "Con PayPal activo utiliza el flujo de aprobación externa (/pagar/iniciar y /pagar/confirmar)."
        );
    }

    @Override
    public ResultadoTransaccionPasarela liberarFondos(String referenciaAutorizacion, BigDecimal monto) {
        if (referenciaAutorizacion == null || referenciaAutorizacion.isBlank()) {
            return new ResultadoTransaccionPasarela(false, null, "No existe una autorización de PayPal para capturar.");
        }
        try {
            String token = obtenerAccessToken();
            Map<String, Object> cuerpo = Map.of(
                    "final_capture", true,
                    "amount", Map.of(
                            "currency_code", MONEDA_USD,
                            "value", formatearMonto(monto)
                    )
            );
            String respJson = restClient.post()
                    .uri("/v2/payments/authorizations/{authId}/capture", referenciaAutorizacion)
                    .header("Authorization", "Bearer " + token)
                    .header("PayPal-Request-Id", "CAP-" + referenciaAutorizacion)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(cuerpo)
                    .retrieve()
                    .body(String.class);

            JsonNode nodo = leerJson(respJson);
            String captureId = nodo.path("id").asText(null);
            String estado = nodo.path("status").asText("");
            if (captureId == null || (!"COMPLETED".equalsIgnoreCase(estado) && !"PENDING".equalsIgnoreCase(estado))) {
                return new ResultadoTransaccionPasarela(false, captureId,
                        "La captura en PayPal no se completó (estado: " + estado + ").");
            }
            return new ResultadoTransaccionPasarela(true, captureId, null);
        } catch (ExcepcionNegocio e) {
            return new ResultadoTransaccionPasarela(false, null, e.getMessage());
        } catch (RestClientResponseException e) {
            String msg = extraerErrorPayPalSeguro(e, "PayPal rechazó la captura de la autorización.");
            if (e.getResponseBodyAsString().contains(ISSUE_AUTORIZACION_EXPIRADA)) {
                msg = "La autorización en PayPal expiró y requiere reautorización o gestión manual.";
            }
            return new ResultadoTransaccionPasarela(false, null, msg);
        } catch (RestClientException e) {
            return new ResultadoTransaccionPasarela(false, null, "Error de comunicación al capturar fondos en PayPal.");
        }
    }

    @Override
    public ResultadoTransaccionPasarela reembolsarFondos(String referenciaAutorizacion, BigDecimal monto, String motivo) {
        if (referenciaAutorizacion == null || referenciaAutorizacion.isBlank()) {
            return new ResultadoTransaccionPasarela(false, null, "No existe autorización de PayPal para anular.");
        }
        try {
            String token = obtenerAccessToken();
            restClient.post()
                    .uri("/v2/payments/authorizations/{authId}/void", referenciaAutorizacion)
                    .header("Authorization", "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .toBodilessEntity();
            return new ResultadoTransaccionPasarela(true, "VOID-" + referenciaAutorizacion, null);
        } catch (ExcepcionNegocio e) {
            return new ResultadoTransaccionPasarela(false, null, e.getMessage());
        } catch (RestClientResponseException e) {
            return new ResultadoTransaccionPasarela(false, null,
                    extraerErrorPayPalSeguro(e, "No fue posible anular la autorización en PayPal."));
        } catch (RestClientException e) {
            return new ResultadoTransaccionPasarela(false, null, "Error de red al anular la autorización en PayPal.");
        }
    }

    @Override
    public ResultadoTransaccionPasarela reembolsarCaptura(String referenciaCaptura, BigDecimal monto, String motivo) {
        if (referenciaCaptura == null || referenciaCaptura.isBlank()) {
            return new ResultadoTransaccionPasarela(false, null, "No existe captura de PayPal para reembolsar.");
        }
        try {
            String token = obtenerAccessToken();
            Map<String, Object> cuerpo = new LinkedHashMap<>();
            if (monto != null) {
                cuerpo.put("amount", Map.of(
                        "currency_code", MONEDA_USD,
                        "value", formatearMonto(monto)
                ));
            }
            cuerpo.put("note_to_payer", truncar(motivo, MAX_NOTA_REEMBOLSO, "Reembolso aprobado por A La Mano"));

            String respJson = restClient.post()
                    .uri("/v2/payments/captures/{captureId}/refund", referenciaCaptura)
                    .header("Authorization", "Bearer " + token)
                    .header("PayPal-Request-Id", "REF-" + referenciaCaptura)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(cuerpo)
                    .retrieve()
                    .body(String.class);

            JsonNode nodo = leerJson(respJson);
            String refundId = nodo.path("id").asText("REFUND-" + referenciaCaptura);
            String estado = nodo.path("status").asText("COMPLETED");
            if ("FAILED".equalsIgnoreCase(estado) || "CANCELLED".equalsIgnoreCase(estado)) {
                return new ResultadoTransaccionPasarela(false, refundId, "El reembolso en PayPal falló.");
            }
            return new ResultadoTransaccionPasarela(true, refundId, null);
        } catch (ExcepcionNegocio e) {
            return new ResultadoTransaccionPasarela(false, null, e.getMessage());
        } catch (RestClientResponseException e) {
            return new ResultadoTransaccionPasarela(false, null,
                    extraerErrorPayPalSeguro(e, "PayPal rechazó el reembolso de la captura."));
        } catch (RestClientException e) {
            return new ResultadoTransaccionPasarela(false, null, "Error de comunicación al reembolsar en PayPal.");
        }
    }

    private synchronized String obtenerAccessToken() {
        if (tokenAccesoCache != null && Instant.now().isBefore(expiracionTokenCache)) {
            return tokenAccesoCache;
        }
        PropiedadesApp.Paypal cfg = propiedades.getPagos().getPaypal();
        if (cfg.getClientId() == null || cfg.getClientId().isBlank()
                || cfg.getClientSecret() == null || cfg.getClientSecret().isBlank()) {
            throw new ExcepcionNegocio("Faltan PAYPAL_CLIENT_ID y PAYPAL_CLIENT_SECRET en el entorno.");
        }

        String credenciales = cfg.getClientId().trim() + ":" + cfg.getClientSecret().trim();
        String basicAuth = Base64.getEncoder().encodeToString(credenciales.getBytes(StandardCharsets.UTF_8));

        try {
            String respJson = restClient.post()
                    .uri("/v1/oauth2/token")
                    .header("Authorization", "Basic " + basicAuth)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .accept(MediaType.APPLICATION_JSON)
                    .body("grant_type=client_credentials")
                    .retrieve()
                    .body(String.class);

            JsonNode nodo = leerJson(respJson);
            String nuevoToken = nodo.path("access_token").asText(null);
            long expiresIn = nodo.path("expires_in").asLong(32400L);
            if (nuevoToken == null || nuevoToken.isBlank()) {
                throw new ExcepcionNegocio("PayPal no retornó un token OAuth2 válido.");
            }
            this.tokenAccesoCache = nuevoToken;
            this.expiracionTokenCache = Instant.now().plusSeconds(Math.max(1L, expiresIn - MARGEN_EXPIRACION_TOKEN_SEGUNDOS));
            return this.tokenAccesoCache;
        } catch (RestClientResponseException e) {
            throw new ExcepcionNegocio(extraerErrorPayPalSeguro(e, "Credenciales de PayPal rechazadas."));
        } catch (RestClientException e) {
            throw new ExcepcionNegocio("No fue posible autenticar con PayPal Sandbox.");
        }
    }

    private ResultadoAutorizacionPago extraerAutorizacion(JsonNode raizOrden, String idOrden) {
        JsonNode unidades = raizOrden.path("purchase_units");
        if (!unidades.isArray() || unidades.isEmpty()) {
            return new ResultadoAutorizacionPago(false, idOrden, null, null, "La respuesta de PayPal no contiene unidades de compra.");
        }
        JsonNode autorizaciones = unidades.get(0).path("payments").path("authorizations");
        if (!autorizaciones.isArray() || autorizaciones.isEmpty()) {
            return new ResultadoAutorizacionPago(false, idOrden, null, null, "PayPal no generó una autorización retenida.");
        }
        JsonNode auth = autorizaciones.get(0);
        String authId = auth.path("id").asText(null);
        String estado = auth.path("status").asText("");
        if (authId == null || ESTADO_AUTORIZACION_DENEGADA.equalsIgnoreCase(estado)
                || ESTADO_AUTORIZACION_ANULADA.equalsIgnoreCase(estado)) {
            return new ResultadoAutorizacionPago(false, idOrden, authId, null,
                    "La autorización en PayPal fue rechazada (estado: " + estado + ").");
        }
        OffsetDateTime expiraEn = null;
        String expiraTexto = auth.path("expiration_time").asText(null);
        if (expiraTexto != null && !expiraTexto.isBlank()) {
            try {
                expiraEn = OffsetDateTime.parse(expiraTexto);
            } catch (Exception ignored) {
                expiraEn = null;
            }
        }
        return new ResultadoAutorizacionPago(true, idOrden, authId, expiraEn, null);
    }

    private String extraerUrlAprobacion(JsonNode links) {
        if (!links.isArray()) {
            return null;
        }
        String alternativa = null;
        for (JsonNode link : links) {
            String rel = link.path("rel").asText("");
            String href = link.path("href").asText(null);
            if ("payer-action".equalsIgnoreCase(rel)) {
                return href;
            }
            if ("approve".equalsIgnoreCase(rel)) {
                alternativa = href;
            }
        }
        return alternativa;
    }

    private JsonNode leerJson(String raw) {
        try {
            return objectMapper.readTree(raw);
        } catch (Exception e) {
            throw new ExcepcionNegocio("No fue posible interpretar la respuesta JSON de PayPal.");
        }
    }

    private String extraerErrorPayPalSeguro(RestClientResponseException ex, String defecto) {
        try {
            String body = ex.getResponseBodyAsString();
            if (body != null && !body.isBlank()) {
                JsonNode nodo = objectMapper.readTree(body);
                JsonNode details = nodo.path("details");
                if (details.isArray() && !details.isEmpty()) {
                    String issue = details.get(0).path("issue").asText("");
                    String desc = details.get(0).path("description").asText("");
                    if (!desc.isBlank()) {
                        return issue.isBlank() ? desc : issue + ": " + desc;
                    }
                }
                String msg = nodo.path("message").asText("");
                if (!msg.isBlank()) {
                    return msg;
                }
            }
        } catch (Exception ignored) {
            // Ignora fallo de parseo de cuerpo de error
        }
        return defecto + " (HTTP " + ex.getStatusCode().value() + ").";
    }

    private String formatearMonto(BigDecimal monto) {
        return monto.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    private String truncar(String texto, int max, String defecto) {
        if (texto == null || texto.isBlank()) {
            return defecto;
        }
        String limpio = texto.trim();
        return limpio.length() > max ? limpio.substring(0, max) : limpio;
    }
}
