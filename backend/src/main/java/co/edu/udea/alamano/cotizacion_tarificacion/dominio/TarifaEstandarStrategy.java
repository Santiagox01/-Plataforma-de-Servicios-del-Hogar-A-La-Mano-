package co.edu.udea.alamano.cotizacion_tarificacion.dominio;

import co.edu.udea.alamano.comun.configuracion.TarifaConfig;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Component
public class TarifaEstandarStrategy implements ITarificacionStrategy {

    private final TarifaConfig tarifaConfig;

    public TarifaEstandarStrategy(TarifaConfig tarifaConfig) {
        this.tarifaConfig = tarifaConfig;
    }

    @Override
    public boolean aplicaParaUrgencia() {
        return false;
    }

    @Override
    public BigDecimal calcular(BigDecimal tarifaBase) {
        return tarifaBase.multiply(tarifaConfig.getFactorNormal()).setScale(2, RoundingMode.HALF_UP);
    }

    @Override
    public BigDecimal obtenerFactorAplicado() {
        return tarifaConfig.getFactorNormal();
    }
}
