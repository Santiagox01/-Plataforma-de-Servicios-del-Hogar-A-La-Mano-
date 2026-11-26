package co.edu.udea.alamano.gestion_servicios.api;

import co.edu.udea.alamano.gestion_servicios.aplicacion.ServicioBusquedaProfesionales;
import co.edu.udea.alamano.gestion_servicios.dto.PerfilProfesionalPublicoDTO;
import co.edu.udea.alamano.gestion_servicios.dto.ResultadoBusquedaProfesionalesDTO;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/profesionales")
public class ProfesionalBusquedaController {

    private final ServicioBusquedaProfesionales servicioBusqueda;

    public ProfesionalBusquedaController(ServicioBusquedaProfesionales servicioBusqueda) {
        this.servicioBusqueda = servicioBusqueda;
    }

    @GetMapping("/buscar")
    public ResponseEntity<ResultadoBusquedaProfesionalesDTO> buscar(
            @RequestParam Long categoriaId,
            @RequestParam Double latitud,
            @RequestParam Double longitud,
            @RequestParam(required = false) Double radioKm) {
        return ResponseEntity.ok(servicioBusqueda.buscarProfesionales(categoriaId, latitud, longitud, radioKm));
    }

    @GetMapping("/{id}")
    public ResponseEntity<PerfilProfesionalPublicoDTO> consultarPerfilPublico(@PathVariable Long id) {
        return ResponseEntity.ok(servicioBusqueda.consultarPerfilPublico(id));
    }
}
