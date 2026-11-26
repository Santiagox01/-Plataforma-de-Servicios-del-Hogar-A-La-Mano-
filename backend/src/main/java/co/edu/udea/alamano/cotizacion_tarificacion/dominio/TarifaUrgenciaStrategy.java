package co.edu.udea.alamano.cotizacion_tarificacion.dominio;

import co.edu.udea.alamano.comun.configuracion.TarifaConfig;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Component
public class TarifaUrgenciaStrategy implements ITarificacionStrategy {

    private final TarifaConfig tarifaConfig;

    public TarifaUrgenciaStrategy(TarifaConfig tarifaConfig) {
        this.tarifaConfig = tarifaConfig;
    }

    @Override
    public boolean aplicaParaUrgencia() {
        return true;
    }

    @Override
    public BigDecimal calcular(BigDecimal tarifaBase) {
        return tarifaBase.multiply(tarifaConfig.getFactorUrgencia()).setScale(2, RoundingMode.HALF_UP);
    }

    @Override
    public BigDecimal obtenerFactorAplicado() {
        return tarifaConfig.getFactorUrgencia();
    }
}
