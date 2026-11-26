package co.edu.udea.alamano.autenticacion_seguridad.aplicacion;

import co.edu.udea.alamano.autenticacion_seguridad.dominio.Profesional;
import co.edu.udea.alamano.autenticacion_seguridad.dto.ResumenUsuarioDTO;
import co.edu.udea.alamano.autenticacion_seguridad.infraestructura.ProfesionalRepositorio;
import co.edu.udea.alamano.autenticacion_seguridad.infraestructura.RepositorioFactory;
import co.edu.udea.alamano.comun.almacenamiento.IAlmacenamientoArchivos;
import co.edu.udea.alamano.comun.excepcion.ExcepcionRecursoNoEncontrado;
import co.edu.udea.alamano.comun.validacion.ValidadorDatosComunes;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;

@Service
public class ServicioPerfilProfesional {

    private final ProfesionalRepositorio profesionalRepositorio;
    private final IAlmacenamientoArchivos almacenamientoArchivos;
    private final ValidadorDatosComunes validador;
    private final ServicioAutenticacion servicioAutenticacion;

    public ServicioPerfilProfesional(RepositorioFactory repositorioFactory,
                                     IAlmacenamientoArchivos almacenamientoArchivos,
                                     ValidadorDatosComunes validador,
                                     ServicioAutenticacion servicioAutenticacion) {
        this.profesionalRepositorio = repositorioFactory.crearProfesionalRepositorio();
        this.almacenamientoArchivos = almacenamientoArchivos;
        this.validador = validador;
        this.servicioAutenticacion = servicioAutenticacion;
    }

    @Transactional
    public ResumenUsuarioDTO actualizarDisponibilidad(Long idProfesional, boolean disponible) {
        Profesional profesional = profesionalRepositorio.findById(idProfesional)
                .orElseThrow(() -> new ExcepcionRecursoNoEncontrado("Profesional no encontrado."));
        profesional.establecerDisponibilidad(disponible);
        Profesional guardado = profesionalRepositorio.save(profesional);
        return servicioAutenticacion.construirResumen(guardado);
    }

    @Transactional
    public ResumenUsuarioDTO recargarDocumentos(Long idProfesional,
                                                  MultipartFile documentoIdentidad,
                                                  List<MultipartFile> certificaciones) {
        Profesional profesional = profesionalRepositorio.findById(idProfesional)
                .orElseThrow(() -> new ExcepcionRecursoNoEncontrado("Profesional no encontrado."));

        validador.validarArchivoDocumento(documentoIdentidad);
        if (certificaciones != null) {
            for (MultipartFile cert : certificaciones) {
                if (cert != null && !cert.isEmpty()) {
                    validador.validarArchivoDocumento(cert);
                }
            }
        }

        String nuevaRutaDoc = almacenamientoArchivos.guardarArchivoPrivado(documentoIdentidad, "identidades");
        List<String> nuevasCerts = new ArrayList<>();
        if (certificaciones != null) {
            for (MultipartFile cert : certificaciones) {
                if (cert != null && !cert.isEmpty()) {
                    nuevasCerts.add(almacenamientoArchivos.guardarArchivoPrivado(cert, "certificaciones"));
                }
            }
        }

        profesional.recargarDocumentoIdentidad(nuevaRutaDoc, nuevasCerts);
        Profesional guardado = profesionalRepositorio.save(profesional);
        return servicioAutenticacion.construirResumen(guardado);
    }
}
