package co.edu.udea.alamano.pagos.dominio;

import java.math.BigDecimal;

/**
 * Puerto base de pasarela de pagos (patron GoF Adapter / GRASP Variaciones Protegidas).
 */
public interface IPasarelaPagos {

    ResultadoValidacionMetodo validarMetodo(DatosTarjetaValidacion datos);

    ResultadoTransaccionPasarela procesarCobro(String tokenMetodo, BigDecimal montoTotal, String referenciaIdempotencia);

    ResultadoTransaccionPasarela liberarFondos(String referenciaPasarela, BigDecimal monto);

    ResultadoTransaccionPasarela reembolsarFondos(String referenciaPasarela, BigDecimal monto, String motivo);

    default ResultadoTransaccionPasarela reembolsarCaptura(String referenciaCaptura, BigDecimal monto, String motivo) {
        return reembolsarFondos(referenciaCaptura, monto, motivo);
    }

    default String obtenerTipoPasarela() {
        return "simulada";
    }

    default boolean requiereAprobacionExterna() {
        return false;
    }
}
