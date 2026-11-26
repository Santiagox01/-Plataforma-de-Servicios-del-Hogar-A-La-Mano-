package co.edu.udea.alamano.comun.configuracion;

import co.edu.udea.alamano.comun.excepcion.ExcepcionNegocio;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.math.BigDecimal;
import java.math.RoundingMode;

@ConfigurationProperties(prefix = "app.tarifa")
public class TarifaConfig {

    public static final BigDecimal FACTOR_NORMAL_DEFECTO = new BigDecimal("1.00");
    public static final BigDecimal FACTOR_URGENCIA_DEFECTO = new BigDecimal("1.30");
    public static final BigDecimal TASA_GARANTIA_FIJA_DEFECTO = new BigDecimal("3000.00");
    public static final BigDecimal TASA_CAMBIO_COP_USD_DEFECTO = new BigDecimal("4000.00");
    public static final BigDecimal MONTO_MINIMO_USD = new BigDecimal("0.01");
    public static final double RADIO_BUSQUEDA_DEFECTO_KM = 10.0;

    private BigDecimal factorNormal = FACTOR_NORMAL_DEFECTO;
    private BigDecimal factorUrgencia = FACTOR_URGENCIA_DEFECTO;
    private BigDecimal tasaGarantiaFija = TASA_GARANTIA_FIJA_DEFECTO;
    private BigDecimal tasaCambioCopUsd = TASA_CAMBIO_COP_USD_DEFECTO;
    private double radioBusquedaDefectoKm = RADIO_BUSQUEDA_DEFECTO_KM;

    public BigDecimal convertirCopAUsd(BigDecimal montoCop) {
        if (montoCop == null || montoCop.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ExcepcionNegocio("El monto en COP debe ser mayor a cero para convertir a USD.");
        }
        if (tasaCambioCopUsd == null || tasaCambioCopUsd.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ExcepcionNegocio("La tasa de cambio COP a USD configurada debe ser mayor a cero.");
        }
        BigDecimal convertido = montoCop.divide(tasaCambioCopUsd, 2, RoundingMode.HALF_UP);
        return convertido.compareTo(MONTO_MINIMO_USD) < 0 ? MONTO_MINIMO_USD : convertido;
    }

    public BigDecimal getFactorNormal() {
        return factorNormal;
    }

    public void setFactorNormal(BigDecimal factorNormal) {
        this.factorNormal = factorNormal;
    }

    public BigDecimal getFactorUrgencia() {
        return factorUrgencia;
    }

    public void setFactorUrgencia(BigDecimal factorUrgencia) {
        this.factorUrgencia = factorUrgencia;
    }

    public BigDecimal getTasaGarantiaFija() {
        return tasaGarantiaFija;
    }

    public void setTasaGarantiaFija(BigDecimal tasaGarantiaFija) {
        this.tasaGarantiaFija = tasaGarantiaFija;
    }

    public BigDecimal getTasaCambioCopUsd() {
        return tasaCambioCopUsd;
    }

    public void setTasaCambioCopUsd(BigDecimal tasaCambioCopUsd) {
        this.tasaCambioCopUsd = tasaCambioCopUsd;
    }

    public double getRadioBusquedaDefectoKm() {
        return radioBusquedaDefectoKm;
    }

    public void setRadioBusquedaDefectoKm(double radioBusquedaDefectoKm) {
        this.radioBusquedaDefectoKm = radioBusquedaDefectoKm;
    }
}
