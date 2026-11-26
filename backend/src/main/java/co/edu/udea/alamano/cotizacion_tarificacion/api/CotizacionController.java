package co.edu.udea.alamano.cotizacion_tarificacion.api;

import co.edu.udea.alamano.cotizacion_tarificacion.aplicacion.CalculadorTarifa;
import co.edu.udea.alamano.cotizacion_tarificacion.dto.CotizacionDTO;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/cotizaciones")
public class CotizacionController {

    private final CalculadorTarifa calculadorTarifa;

    public CotizacionController(CalculadorTarifa calculadorTarifa) {
        this.calculadorTarifa = calculadorTarifa;
    }

    @GetMapping("/estimar")
    public ResponseEntity<CotizacionDTO> estimar(@RequestParam Long servicioId,
                                                 @RequestParam(defaultValue = "false") boolean esUrgente) {
        return ResponseEntity.ok(calculadorTarifa.cotizarPorServicioId(servicioId, esUrgente));
    }
}
