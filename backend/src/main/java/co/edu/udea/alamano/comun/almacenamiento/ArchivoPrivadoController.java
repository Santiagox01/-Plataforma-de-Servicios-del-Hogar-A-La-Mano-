package co.edu.udea.alamano.comun.almacenamiento;

import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/archivos/privado")
public class ArchivoPrivadoController {

    private final IAlmacenamientoArchivos almacenamientoArchivos;

    public ArchivoPrivadoController(IAlmacenamientoArchivos almacenamientoArchivos) {
        this.almacenamientoArchivos = almacenamientoArchivos;
    }

    @GetMapping("/{token}")
    public ResponseEntity<byte[]> visualizarInline(@PathVariable String token) {
        RecursoArchivo recurso = almacenamientoArchivos.obtenerArchivoPorToken(token);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(recurso.tipoContenido()));
        headers.setContentDisposition(ContentDisposition.inline().filename(recurso.nombreArchivo()).build());
        headers.setCacheControl(CacheControl.noStore());
        headers.add("X-Content-Type-Options", "nosniff");
        return ResponseEntity.ok().headers(headers).body(recurso.contenido());
    }
}
