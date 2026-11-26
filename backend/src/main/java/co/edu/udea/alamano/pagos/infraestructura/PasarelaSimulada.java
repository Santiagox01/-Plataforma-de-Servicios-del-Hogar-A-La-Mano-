package co.edu.udea.alamano.pagos.infraestructura;

import co.edu.udea.alamano.pagos.dominio.DatosTarjetaValidacion;
import co.edu.udea.alamano.pagos.dominio.FranquiciaTarjeta;
import co.edu.udea.alamano.pagos.dominio.IPasarelaPagos;
import co.edu.udea.alamano.pagos.dominio.ResultadoTransaccionPasarela;
import co.edu.udea.alamano.pagos.dominio.ResultadoValidacionMetodo;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.Locale;
import java.util.UUID;

/**
 * Implementacion simulada de IPasarelaPagos para el MVP.
 * Tarjetas de prueba documentadas:
 *  - Terminada en 0001: rechazada durante el registro.
 *  - Terminada en 0002: aprobada en registro, rechazada al cobrar por fondos insuficientes.
 *  - Cualquier otra tarjeta valida no vencida (ej. 4242424242424242): aprobada en registro y cobro.
 */
@Component
@ConditionalOnProperty(name = "app.pagos.pasarela", havingValue = "simulada", matchIfMissing = true)
public class PasarelaSimulada implements IPasarelaPagos {

    public static final String SUFIJO_RECHAZO_REGISTRO = "0001";
    public static final String SUFIJO_FONDOS_INSUFICIENTES = "0002";
    public static final String PREFIJO_TOKEN_SIN_FONDOS = "tok_sim_nf_";
    public static final String PREFIJO_TOKEN_OK = "tok_sim_ok_";

    private static final int MIN_DIGITOS_TARJETA = 13;
    private static final int MAX_DIGITOS_TARJETA = 19;
    private static final int MIN_DIGITOS_CVV = 3;
    private static final int MAX_DIGITOS_CVV = 4;
    private static final int LONGITUD_SUFIJO = 4;

    @Override
    public String obtenerTipoPasarela() {
        return "simulada";
    }

    @Override
    public ResultadoValidacionMetodo validarMetodo(DatosTarjetaValidacion datos) {
        String soloDigitos = datos.numeroTarjeta() == null
                ? ""
                : datos.numeroTarjeta().replaceAll("\\D", "");
        String cvvLimpio = datos.cvv() == null ? "" : datos.cvv().replaceAll("\\D", "");

        if (soloDigitos.length() < MIN_DIGITOS_TARJETA || soloDigitos.length() > MAX_DIGITOS_TARJETA) {
            return new ResultadoValidacionMetodo(false, null, null, null,
                    "El número de tarjeta debe contener entre 13 y 19 dígitos.");
        }
        if (cvvLimpio.length() < MIN_DIGITOS_CVV || cvvLimpio.length() > MAX_DIGITOS_CVV) {
            return new ResultadoValidacionMetodo(false, null, null, null,
                    "El código de seguridad CVV debe tener 3 o 4 dígitos.");
        }
        if (YearMonth.of(datos.anioVencimiento(), datos.mesVencimiento()).isBefore(YearMonth.now())) {
            return new ResultadoValidacionMetodo(false, null, null, null,
                    "La tarjeta se encuentra vencida.");
        }

        String ultimosDigitos = soloDigitos.substring(soloDigitos.length() - LONGITUD_SUFIJO);
        if (SUFIJO_RECHAZO_REGISTRO.equals(ultimosDigitos)) {
            return new ResultadoValidacionMetodo(false, null, null, null,
                    "Tarjeta rechazada por el emisor al validar el método de pago.");
        }

        FranquiciaTarjeta franquicia = detectarFranquicia(soloDigitos);
        String prefijoToken = SUFIJO_FONDOS_INSUFICIENTES.equals(ultimosDigitos)
                ? PREFIJO_TOKEN_SIN_FONDOS
                : PREFIJO_TOKEN_OK;
        String token = prefijoToken + UUID.randomUUID().toString().replace("-", "");

        return new ResultadoValidacionMetodo(true, token, franquicia, ultimosDigitos, null);
    }

    @Override
    public ResultadoTransaccionPasarela procesarCobro(String tokenMetodo,
                                                      BigDecimal montoTotal,
                                                      String referenciaIdempotencia) {
        if (tokenMetodo == null || tokenMetodo.isBlank()) {
            return new ResultadoTransaccionPasarela(false, null, "Token de pago inválido o inexistente.");
        }
        if (montoTotal == null || montoTotal.compareTo(BigDecimal.ZERO) <= 0) {
            return new ResultadoTransaccionPasarela(false, null, "El monto a cobrar debe ser mayor a cero.");
        }
        if (tokenMetodo.startsWith(PREFIJO_TOKEN_SIN_FONDOS)) {
            return new ResultadoTransaccionPasarela(false, null, "Fondos insuficientes en la tarjeta seleccionada.");
        }
        String refPasarela = "SIM-TX-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(Locale.ROOT);
        return new ResultadoTransaccionPasarela(true, refPasarela, null);
    }

    @Override
    public ResultadoTransaccionPasarela liberarFondos(String referenciaPasarela, BigDecimal monto) {
        if (referenciaPasarela == null || referenciaPasarela.isBlank()) {
            return new ResultadoTransaccionPasarela(false, null, "Referencia de transacción en custodia inválida.");
        }
        return new ResultadoTransaccionPasarela(true, referenciaPasarela, null);
    }

    @Override
    public ResultadoTransaccionPasarela reembolsarFondos(String referenciaPasarela, BigDecimal monto, String motivo) {
        if (referenciaPasarela == null || referenciaPasarela.isBlank()) {
            return new ResultadoTransaccionPasarela(false, null, "No existe referencia de pasarela para reembolsar.");
        }
        return new ResultadoTransaccionPasarela(true, "SIM-REEMB-" + referenciaPasarela, null);
    }

    private FranquiciaTarjeta detectarFranquicia(String digitos) {
        if (digitos.startsWith("4")) {
            return FranquiciaTarjeta.VISA;
        }
        if (digitos.startsWith("5") || digitos.startsWith("2")) {
            return FranquiciaTarjeta.MASTERCARD;
        }
        if (digitos.startsWith("34") || digitos.startsWith("37")) {
            return FranquiciaTarjeta.AMEX;
        }
        return FranquiciaTarjeta.OTRA;
    }
}
