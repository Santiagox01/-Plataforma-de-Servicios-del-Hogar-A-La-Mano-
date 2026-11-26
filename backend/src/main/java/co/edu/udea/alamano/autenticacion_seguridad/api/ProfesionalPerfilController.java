package co.edu.udea.alamano.autenticacion_seguridad.api;

import co.edu.udea.alamano.autenticacion_seguridad.aplicacion.ServicioPerfilProfesional;
import co.edu.udea.alamano.autenticacion_seguridad.dto.DisponibilidadDTO;
import co.edu.udea.alamano.autenticacion_seguridad.dto.ResumenUsuarioDTO;
import co.edu.udea.alamano.autenticacion_seguridad.seguridad.DetallesUsuario;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/profesionales/me")
@PreAuthorize("hasRole('PROFESIONAL')")
public class ProfesionalPerfilController {

    private final ServicioPerfilProfesional servicioPerfilProfesional;

    public ProfesionalPerfilController(ServicioPerfilProfesional servicioPerfilProfesional) {
        this.servicioPerfilProfesional = servicioPerfilProfesional;
    }

    @PatchMapping("/disponibilidad")
    public ResponseEntity<ResumenUsuarioDTO> actualizarDisponibilidad(@Valid @RequestBody DisponibilidadDTO solicitud,
                                                                      @AuthenticationPrincipal DetallesUsuario profesional) {
        return ResponseEntity.ok(servicioPerfilProfesional.actualizarDisponibilidad(profesional.getIdUsuario(), solicitud.disponible()));
    }

    @PostMapping(value = "/documentos", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ResumenUsuarioDTO> recargarDocumentos(
            @AuthenticationPrincipal DetallesUsuario profesional,
            @RequestPart("documentoIdentidad") MultipartFile documentoIdentidad,
            @RequestPart(value = "certificaciones", required = false) List<MultipartFile> certificaciones) {
        return ResponseEntity.ok(servicioPerfilProfesional.recargarDocumentos(
                profesional.getIdUsuario(), documentoIdentidad, certificaciones));
    }
}
