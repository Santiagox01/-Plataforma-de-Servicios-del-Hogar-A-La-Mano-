package co.edu.udea.alamano.pagos.infraestructura;

import co.edu.udea.alamano.pagos.dominio.DatosTarjetaValidacion;
import co.edu.udea.alamano.pagos.dominio.IPasarelaPagos;
import co.edu.udea.alamano.pagos.dominio.ResultadoTransaccionPasarela;
import co.edu.udea.alamano.pagos.dominio.ResultadoValidacionMetodo;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Esqueleto de adaptador para la pasarela comercial Wompi (patron GoF Adapter).
 * Activo unicamente cuando {@code app.pagos.pasarela=wompi}.
 * <p>
 * Integracion productiva prevista:
 * - Tokenizacion segura via POST /v1/tokens/cards con llave publica.
 * - Cobro en centavos COP y firma de integridad SHA-256 contra /v1/transactions.
 * - Recepcion asincrona de webhooks para confirmar eventos APPROVED/DECLINED/VOIDED.
 */
@Component
@ConditionalOnProperty(name = "app.pagos.pasarela", havingValue = "wompi")
public class WompiAdapter implements IPasarelaPagos {

    @Override
    public String obtenerTipoPasarela() {
        return "wompi";
    }

    @Override
    public ResultadoValidacionMetodo validarMetodo(DatosTarjetaValidacion datos) {
        throw new UnsupportedOperationException("WompiAdapter requiere configurar credenciales productivas de Wompi.");
    }

    @Override
    public ResultadoTransaccionPasarela procesarCobro(String tokenMetodo,
                                                      BigDecimal montoTotal,
                                                      String referenciaIdempotencia) {
        throw new UnsupportedOperationException("WompiAdapter requiere configurar credenciales productivas de Wompi.");
    }

    @Override
    public ResultadoTransaccionPasarela liberarFondos(String referenciaPasarela, BigDecimal monto) {
        throw new UnsupportedOperationException("WompiAdapter requiere configurar dispersión productiva en Wompi.");
    }

    @Override
    public ResultadoTransaccionPasarela reembolsarFondos(String referenciaPasarela, BigDecimal monto, String motivo) {
        throw new UnsupportedOperationException("WompiAdapter requiere configurar reembolsos productivos en Wompi.");
    }
}
