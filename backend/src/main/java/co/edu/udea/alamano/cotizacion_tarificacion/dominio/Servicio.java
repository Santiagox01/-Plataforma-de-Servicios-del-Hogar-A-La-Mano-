package co.edu.udea.alamano.cotizacion_tarificacion.dominio;

import co.edu.udea.alamano.autenticacion_seguridad.dominio.Profesional;
import co.edu.udea.alamano.comun.excepcion.ExcepcionNegocio;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Entity
@Table(name = "servicios")
public class Servicio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_servicio")
    private Long idServicio;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_profesional", nullable = false)
    private Profesional profesional;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "id_categoria", nullable = false)
    private CategoriaOficio categoriaOficio;

    @Column(name = "tarifa_base", precision = 12, scale = 2)
    private BigDecimal tarifaBase;

    @Column(name = "precio_min", precision = 12, scale = 2)
    private BigDecimal precioMin;

    @Column(name = "precio_max", precision = 12, scale = 2)
    private BigDecimal precioMax;

    protected Servicio() {
    }

    public Servicio(Profesional profesional, CategoriaOficio categoriaOficio) {
        this.profesional = profesional;
        this.categoriaOficio = categoriaOficio;
    }

    public void configurarTarifa(BigDecimal base, BigDecimal min, BigDecimal max) {
        if (base == null || min == null || max == null) {
            throw new ExcepcionNegocio("La tarifa base, el precio mínimo y el precio máximo son obligatorios.");
        }
        if (base.compareTo(BigDecimal.ZERO) <= 0 || min.compareTo(BigDecimal.ZERO) <= 0 || max.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ExcepcionNegocio("Los valores de tarifa deben ser mayores a cero.");
        }
        if (min.compareTo(max) >= 0) {
            throw new ExcepcionNegocio("El precio mínimo debe ser estrictamente menor que el precio máximo.");
        }
        if (base.compareTo(min) < 0 || base.compareTo(max) > 0) {
            throw new ExcepcionNegocio("La tarifa base debe estar dentro del rango [precioMin, precioMax].");
        }
        this.tarifaBase = base.setScale(2, RoundingMode.HALF_UP);
        this.precioMin = min.setScale(2, RoundingMode.HALF_UP);
        this.precioMax = max.setScale(2, RoundingMode.HALF_UP);
    }

    public BigDecimal ajustarALimites(BigDecimal precio) {
        if (precioMin == null || precioMax == null) {
            throw new ExcepcionNegocio("El profesional aún no ha configurado tarifas para esta categoría.");
        }
        if (precio.compareTo(precioMin) < 0) {
            return precioMin;
        }
        if (precio.compareTo(precioMax) > 0) {
            return precioMax;
        }
        return precio.setScale(2, RoundingMode.HALF_UP);
    }

    public boolean tieneTarifaConfigurada() {
        return tarifaBase != null && precioMin != null && precioMax != null;
    }

    public ResultadoCotizacion calcularPrecioEstimado(boolean esUrgente, TarificadorContext tarificadorContext) {
        if (!tieneTarifaConfigurada()) {
            throw new ExcepcionNegocio("El profesional aún no ha configurado tarifas para la categoría " + categoriaOficio.getNombre() + ".");
        }
        ITarificacionStrategy estrategia = tarificadorContext.obtenerEstrategia(esUrgente);
        BigDecimal precioSinAcotar = estrategia.calcular(tarifaBase);
        BigDecimal precioAcotado = ajustarALimites(precioSinAcotar);
        boolean acotadoMax = precioSinAcotar.compareTo(precioMax) > 0;
        boolean acotadoMin = precioSinAcotar.compareTo(precioMin) < 0;

        String detalle;
        if (esUrgente && acotadoMax) {
            detalle = "Recargo por urgencia aplicado (factor " + estrategia.obtenerFactorAplicado()
                    + ") y ajustado al tope máximo configurado (" + precioMax + ").";
        } else if (esUrgente) {
            detalle = "Incluye recargo por urgencia (factor " + estrategia.obtenerFactorAplicado() + ").";
        } else {
            detalle = "Tarifa base estándar aplicada (sin recargo de urgencia).";
        }

        return new ResultadoCotizacion(
                tarifaBase,
                precioMin,
                precioMax,
                estrategia.obtenerFactorAplicado(),
                precioSinAcotar,
                precioAcotado,
                esUrgente,
                esUrgente,
                acotadoMax,
                acotadoMin,
                detalle
        );
    }

    public Long getIdServicio() {
        return idServicio;
    }

    public Profesional getProfesional() {
        return profesional;
    }

    public CategoriaOficio getCategoriaOficio() {
        return categoriaOficio;
    }

    public BigDecimal getTarifaBase() {
        return tarifaBase;
    }

    public BigDecimal getPrecioMin() {
        return precioMin;
    }

    public BigDecimal getPrecioMax() {
        return precioMax;
    }
}
