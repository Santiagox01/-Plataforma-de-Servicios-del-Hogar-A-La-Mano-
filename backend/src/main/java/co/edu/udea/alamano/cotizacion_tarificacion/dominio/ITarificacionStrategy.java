package co.edu.udea.alamano.cotizacion_tarificacion.dominio;

import java.math.BigDecimal;

/**
 * Estrategia de tarificacion (patron GoF Strategy).
 */
public interface ITarificacionStrategy {

    boolean aplicaParaUrgencia();

    BigDecimal calcular(BigDecimal tarifaBase);

    BigDecimal obtenerFactorAplicado();
}
