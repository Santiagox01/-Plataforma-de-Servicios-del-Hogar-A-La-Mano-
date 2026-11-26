package co.edu.udea.alamano.cotizacion_tarificacion.aplicacion;

import co.edu.udea.alamano.comun.configuracion.TarifaConfig;
import co.edu.udea.alamano.comun.excepcion.ExcepcionRecursoNoEncontrado;
import co.edu.udea.alamano.cotizacion_tarificacion.dominio.ResultadoCotizacion;
import co.edu.udea.alamano.cotizacion_tarificacion.dominio.Servicio;
import co.edu.udea.alamano.cotizacion_tarificacion.dominio.TarificadorContext;
import co.edu.udea.alamano.cotizacion_tarificacion.dto.CotizacionDTO;
import co.edu.udea.alamano.cotizacion_tarificacion.infraestructura.ServicioRepositorio;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
public class CalculadorTarifa {

    private final ServicioRepositorio servicioRepositorio;
    private final TarificadorContext tarificadorContext;
    private final TarifaConfig tarifaConfig;

    public CalculadorTarifa(ServicioRepositorio servicioRepositorio,
                            TarificadorContext tarificadorContext,
                            TarifaConfig tarifaConfig) {
        this.servicioRepositorio = servicioRepositorio;
        this.tarificadorContext = tarificadorContext;
        this.tarifaConfig = tarifaConfig;
    }

    public BigDecimal calcularPrecioEstimado(Servicio servicio, boolean esUrgente) {
        return servicio.calcularPrecioEstimado(esUrgente, tarificadorContext).precioEstimado();
    }

    public ResultadoCotizacion calcularDetalle(Servicio servicio, boolean esUrgente) {
        return servicio.calcularPrecioEstimado(esUrgente, tarificadorContext);
    }

    @Transactional(readOnly = true)
    public CotizacionDTO cotizarPorServicioId(Long idServicio, boolean esUrgente) {
        Servicio servicio = servicioRepositorio.findById(idServicio)
                .orElseThrow(() -> new ExcepcionRecursoNoEncontrado("Servicio no encontrado."));
        ResultadoCotizacion res = servicio.calcularPrecioEstimado(esUrgente, tarificadorContext);
        return new CotizacionDTO(
                servicio.getIdServicio(),
                servicio.getCategoriaOficio().getNombre(),
                res.tarifaBase(),
                res.precioMin(),
                res.precioMax(),
                res.factorAplicado(),
                res.precioEstimado(),
                tarifaConfig.getTasaGarantiaFija(),
                res.esUrgente(),
                res.recargoUrgenciaAplicado(),
                res.acotadoAlMaximo(),
                res.acotadoAlMinimo(),
                res.detalleCalculo()
        );
    }
}
