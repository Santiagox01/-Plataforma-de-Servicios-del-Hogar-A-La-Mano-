package co.edu.udea.alamano.pagos.dominio;

import java.math.BigDecimal;

/**
 * Extension segregada (ISP) para pasarelas basadas en redireccion y autorizacion/captura en dos pasos (ej. PayPal).
 */
public interface IPasarelaPagoRedireccion extends IPasarelaPagos {

    ResultadoInicioPago iniciarPago(BigDecimal montoExterno,
                                    String moneda,
                                    String referenciaIdempotencia,
                                    String descripcion,
                                    String urlRetornoPersonalizada,
                                    String urlCancelacionPersonalizada);

    ResultadoAutorizacionPago confirmarYAutorizarPago(String idOrdenExterna, String referenciaIdempotencia);

    @Override
    default boolean requiereAprobacionExterna() {
        return true;
    }
}
