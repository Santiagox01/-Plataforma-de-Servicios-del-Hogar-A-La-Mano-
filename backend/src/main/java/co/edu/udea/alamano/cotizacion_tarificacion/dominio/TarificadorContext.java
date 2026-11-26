package co.edu.udea.alamano.cotizacion_tarificacion.dominio;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Contexto del patron Strategy para seleccionar la estrategia sin condicionales de negocio.
 */
@Component
public class TarificadorContext {

    private final Map<Boolean, ITarificacionStrategy> estrategias;

    public TarificadorContext(List<ITarificacionStrategy> listaEstrategias) {
        this.estrategias = listaEstrategias.stream()
                .collect(Collectors.toMap(ITarificacionStrategy::aplicaParaUrgencia, Function.identity()));
    }

    public ITarificacionStrategy obtenerEstrategia(boolean esUrgente) {
        ITarificacionStrategy estrategia = estrategias.get(esUrgente);
        if (estrategia == null) {
            throw new IllegalStateException("No hay estrategia de tarificación registrada para urgencia=" + esUrgente);
        }
        return estrategia;
    }
}
