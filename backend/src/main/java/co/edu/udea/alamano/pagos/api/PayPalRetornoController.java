package co.edu.udea.alamano.pagos.api;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.HtmlUtils;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

/**
 * Callback HTTP publico para recibir el retorno del navegador desde PayPal Sandbox y redirigir a la app Expo.
 * Nunca aprueba ni confirma pagos sin autenticacion JWT del cliente en /pagar/confirmar.
 */
@RestController
@RequestMapping("/api/pagos/paypal")
public class PayPalRetornoController {

    private static final Pattern PATRON_APP_REDIRECT = Pattern.compile("^(alamano|exp)://[A-Za-z0-9._~:/?#\\[\\]@!$&'()*+,;=%-]+$");

    @GetMapping(value = "/retorno", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> retornoAprobado(@RequestParam(required = false) String token,
                                                  @RequestParam(required = false) String appRedirect) {
        return ResponseEntity.ok(construirHtml("aprobado", token, appRedirect,
                "Aprobación registrada en PayPal",
                "Ahora regresa a la app A La Mano para finalizar la validación en custodia."));
    }

    @GetMapping(value = "/cancelado", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> retornoCancelado(@RequestParam(required = false) String token,
                                                   @RequestParam(required = false) String appRedirect) {
        return ResponseEntity.ok(construirHtml("cancelado", token, appRedirect,
                "Pago cancelado en PayPal",
                "Cancelaste la operación antes de autorizar. Puedes regresar a la app para intentarlo de nuevo."));
    }

    private String construirHtml(String estado,
                                 String orderId,
                                 String appRedirect,
                                 String titulo,
                                 String mensaje) {
        String deepLink = construirDeepLinkSeguro(appRedirect, estado, orderId);
        String tituloSeguro = HtmlUtils.htmlEscape(titulo);
        String mensajeSeguro = HtmlUtils.htmlEscape(mensaje);
        String ordenSegura = orderId != null ? HtmlUtils.htmlEscape(orderId) : "";

        if (deepLink != null) {
            String deepLinkHtml = HtmlUtils.htmlEscape(deepLink);
            String deepLinkJs = escaparParaJsString(deepLink);
            return """
                    <!DOCTYPE html>
                    <html lang="es">
                    <head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1"><title>A La Mano - PayPal</title></head>
                    <body style="font-family:sans-serif;padding:24px;text-align:center;background:#f8fafc;color:#0f172a;">
                      <h2>%s</h2>
                      <p>%s</p>
                      <p style="font-size:12px;color:#475569;">Orden: %s</p>
                      <a href="%s" style="display:inline-block;padding:12px 20px;background:#b80035;color:#fff;border-radius:8px;text-decoration:none;font-weight:bold;">Volver a A La Mano</a>
                      <script>window.location.replace("%s");</script>
                    </body>
                    </html>
                    """.formatted(tituloSeguro, mensajeSeguro, ordenSegura, deepLinkHtml, deepLinkJs);
        }

        return """
                <!DOCTYPE html>
                <html lang="es">
                <head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1"><title>A La Mano - PayPal</title></head>
                <body style="font-family:sans-serif;padding:24px;text-align:center;background:#f8fafc;color:#0f172a;">
                  <h2>%s</h2>
                  <p>%s</p>
                  <p style="font-size:12px;color:#475569;">Orden: %s</p>
                  <p>Cierra esta ventana del navegador para continuar en A La Mano.</p>
                </body>
                </html>
                """.formatted(tituloSeguro, mensajeSeguro, ordenSegura);
    }

    private String construirDeepLinkSeguro(String appRedirect, String estado, String orderId) {
        if (appRedirect == null || appRedirect.isBlank() || !PATRON_APP_REDIRECT.matcher(appRedirect.trim()).matches()) {
            return null;
        }
        String base = appRedirect.trim();
        String sep = base.contains("?") ? "&" : "?";
        StringBuilder sb = new StringBuilder(base)
                .append(sep)
                .append("estado=")
                .append(URLEncoder.encode(estado, StandardCharsets.UTF_8));
        if (orderId != null && !orderId.isBlank()) {
            sb.append("&orderId=").append(URLEncoder.encode(orderId.trim(), StandardCharsets.UTF_8));
        }
        return sb.toString();
    }

    private String escaparParaJsString(String valor) {
        return valor
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("<", "\\u003C")
                .replace(">", "\\u003E");
    }
}
