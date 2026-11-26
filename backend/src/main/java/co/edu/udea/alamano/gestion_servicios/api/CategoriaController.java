package co.edu.udea.alamano.gestion_servicios.api;

import co.edu.udea.alamano.gestion_servicios.aplicacion.ServicioBusquedaProfesionales;
import co.edu.udea.alamano.gestion_servicios.dto.CategoriaDTO;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/categorias")
public class CategoriaController {

    private final ServicioBusquedaProfesionales servicioBusqueda;

    public CategoriaController(ServicioBusquedaProfesionales servicioBusqueda) {
        this.servicioBusqueda = servicioBusqueda;
    }

    @GetMapping
    public ResponseEntity<List<CategoriaDTO>> listarCategorias() {
        return ResponseEntity.ok(servicioBusqueda.listarCategorias());
    }
}
