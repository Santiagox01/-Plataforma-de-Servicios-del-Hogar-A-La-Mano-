package co.edu.udea.alamano.autenticacion_seguridad.api;

import co.edu.udea.alamano.autenticacion_seguridad.aplicacion.ServicioAutenticacion;
import co.edu.udea.alamano.autenticacion_seguridad.dto.RespuestaAuthDTO;
import co.edu.udea.alamano.autenticacion_seguridad.dto.ResumenUsuarioDTO;
import co.edu.udea.alamano.autenticacion_seguridad.dto.SolicitudLoginDTO;
import co.edu.udea.alamano.autenticacion_seguridad.dto.SolicitudRegistroClienteDTO;
import co.edu.udea.alamano.autenticacion_seguridad.seguridad.DetallesUsuario;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final ServicioAutenticacion servicioAutenticacion;

    public AuthController(ServicioAutenticacion servicioAutenticacion) {
        this.servicioAutenticacion = servicioAutenticacion;
    }

    @PostMapping("/registro/cliente")
    public ResponseEntity<RespuestaAuthDTO> registrarCliente(@Valid @RequestBody SolicitudRegistroClienteDTO solicitud) {
        return ResponseEntity.status(HttpStatus.CREATED).body(servicioAutenticacion.registrarCliente(solicitud));
    }

    @PostMapping(value = "/registro/profesional", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<RespuestaAuthDTO> registrarProfesional(
            @RequestParam String nombre,
            @RequestParam String correo,
            @RequestParam String celular,
            @RequestParam String contrasena,
            @RequestParam String ubicacion,
            @RequestParam Double latitud,
            @RequestParam Double longitud,
            @RequestParam List<Long> categoriasIds,
            @RequestParam(defaultValue = "false") boolean aceptaTerminos,
            @RequestPart("documentoIdentidad") MultipartFile documentoIdentidad,
            @RequestPart(value = "certificaciones", required = false) List<MultipartFile> certificaciones) {
        RespuestaAuthDTO respuesta = servicioAutenticacion.registrarProfesional(
                nombre,
                correo,
                celular,
                contrasena,
                ubicacion,
                latitud,
                longitud,
                categoriasIds,
                aceptaTerminos,
                documentoIdentidad,
                certificaciones
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(respuesta);
    }

    @PostMapping("/login")
    public ResponseEntity<RespuestaAuthDTO> iniciarSesion(@Valid @RequestBody SolicitudLoginDTO solicitud) {
        return ResponseEntity.ok(servicioAutenticacion.iniciarSesion(solicitud));
    }

    @PostMapping("/logout")
    public ResponseEntity<Map<String, String>> cerrarSesion(
            @RequestHeader(value = "Authorization", required = false) String cabeceraAutorizacion) {
        servicioAutenticacion.cerrarSesion(cabeceraAutorizacion);
        return ResponseEntity.ok(Map.of("mensaje", "Sesión cerrada correctamente."));
    }

    @GetMapping("/me")
    public ResponseEntity<ResumenUsuarioDTO> miPerfil(@AuthenticationPrincipal DetallesUsuario usuario) {
        return ResponseEntity.ok(servicioAutenticacion.obtenerMiPerfil(usuario.getIdUsuario()));
    }
}
