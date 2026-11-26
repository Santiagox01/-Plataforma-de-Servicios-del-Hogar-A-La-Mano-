package co.edu.udea.alamano.autenticacion_seguridad.aplicacion;

import co.edu.udea.alamano.autenticacion_seguridad.dominio.Administrador;
import co.edu.udea.alamano.autenticacion_seguridad.dominio.EstadoVerificacion;
import co.edu.udea.alamano.autenticacion_seguridad.dominio.Profesional;
import co.edu.udea.alamano.autenticacion_seguridad.dto.ProfesionalPendienteDTO;
import co.edu.udea.alamano.autenticacion_seguridad.dto.ResultadoVerificacionDTO;
import co.edu.udea.alamano.autenticacion_seguridad.dto.UrlDocumentoDTO;
import co.edu.udea.alamano.autenticacion_seguridad.infraestructura.AdministradorRepositorio;
import co.edu.udea.alamano.autenticacion_seguridad.infraestructura.ProfesionalRepositorio;
import co.edu.udea.alamano.autenticacion_seguridad.infraestructura.RepositorioFactory;
import co.edu.udea.alamano.comun.almacenamiento.IAlmacenamientoArchivos;
import co.edu.udea.alamano.comun.auditoria.IComandoAdministrativo;
import co.edu.udea.alamano.comun.auditoria.ServicioAuditoria;
import co.edu.udea.alamano.comun.configuracion.PropiedadesApp;
import co.edu.udea.alamano.comun.excepcion.ExcepcionRecursoNoEncontrado;
import co.edu.udea.alamano.comun.notificacion.ServicioNotificaciones;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ServicioVerificacion {

    private final ProfesionalRepositorio profesionalRepositorio;
    private final AdministradorRepositorio administradorRepositorio;
    private final IAlmacenamientoArchivos almacenamientoArchivos;
    private final ServicioAuditoria servicioAuditoria;
    private final ServicioNotificaciones servicioNotificaciones;
    private final PropiedadesApp propiedades;

    public ServicioVerificacion(RepositorioFactory repositorioFactory,
                                IAlmacenamientoArchivos almacenamientoArchivos,
                                ServicioAuditoria servicioAuditoria,
                                ServicioNotificaciones servicioNotificaciones,
                                PropiedadesApp propiedades) {
        this.profesionalRepositorio = repositorioFactory.crearProfesionalRepositorio();
        this.administradorRepositorio = repositorioFactory.crearAdministradorRepositorio();
        this.almacenamientoArchivos = almacenamientoArchivos;
        this.servicioAuditoria = servicioAuditoria;
        this.servicioNotificaciones = servicioNotificaciones;
        this.propiedades = propiedades;
    }

    @Transactional(readOnly = true)
    public List<ProfesionalPendienteDTO> listarPendientes() {
        return profesionalRepositorio.findByEstadoVerificacionOrderByFechaRegistroAsc(EstadoVerificacion.PENDIENTE_VERIFICACION)
                .stream()
                .map(p -> new ProfesionalPendienteDTO(
                        p.getIdUsuario(),
                        p.getNombre(),
                        p.getCorreo(),
                        p.getCelular(),
                        p.getUbicacion(),
                        p.getEstadoVerificacion(),
                        p.getFechaRegistro(),
                        p.getServicios().stream().map(s -> s.getCategoriaOficio().getNombre()).toList(),
                        p.getDocumentosCertificacion().size()
                ))
                .toList();
    }

    @Transactional(readOnly = true)
    public UrlDocumentoDTO generarUrlsDocumentos(Long idProfesional) {
        Profesional profesional = buscarProfesional(idProfesional);
        String urlIdentidad = almacenamientoArchivos.generarUrlFirmadaTemporal(profesional.getDocumentoIdentidad());
        List<String> urlsCertificaciones = profesional.getDocumentosCertificacion()
                .stream()
                .map(almacenamientoArchivos::generarUrlFirmadaTemporal)
                .toList();
        return new UrlDocumentoDTO(
                profesional.getIdUsuario(),
                urlIdentidad,
                urlsCertificaciones,
                propiedades.getStorage().getExpiracionUrlSegundos()
        );
    }

    @Transactional
    public ResultadoVerificacionDTO aprobarProfesional(Long idProfesional, Long idAdmin) {
        IComandoAdministrativo<ResultadoVerificacionDTO> comando = () -> {
            Profesional profesional = buscarProfesional(idProfesional);
            Administrador admin = buscarAdmin(idAdmin);

            profesional.verificar(admin);
            Profesional guardado = profesionalRepositorio.save(profesional);

            servicioAuditoria.registrar(
                    admin.getIdUsuario(),
                    "APROBAR_PROFESIONAL",
                    "PROFESIONAL",
                    guardado.getIdUsuario(),
                    "Identidad y documentación aprobadas para " + guardado.getCorreo()
            );

            servicioNotificaciones.notificar(
                    guardado.getIdUsuario(),
                    "Verificación aprobada",
                    "Tu identidad ha sido verificada. Ya puedes activar tu disponibilidad para aparecer en las búsquedas.",
                    "VERIFICACION_APROBADA"
            );

            return new ResultadoVerificacionDTO(
                    guardado.getIdUsuario(),
                    guardado.getNombre(),
                    guardado.getEstadoVerificacion(),
                    guardado.getMotivoRechazo(),
                    guardado.getFechaVerificacion(),
                    guardado.isDisponible()
            );
        };
        return comando.ejecutar();
    }

    @Transactional
    public ResultadoVerificacionDTO rechazarProfesional(Long idProfesional, Long idAdmin, String motivo) {
        IComandoAdministrativo<ResultadoVerificacionDTO> comando = () -> {
            Profesional profesional = buscarProfesional(idProfesional);
            Administrador admin = buscarAdmin(idAdmin);

            profesional.rechazarVerificacion(admin, motivo);
            Profesional guardado = profesionalRepositorio.save(profesional);

            servicioAuditoria.registrar(
                    admin.getIdUsuario(),
                    "RECHAZAR_PROFESIONAL",
                    "PROFESIONAL",
                    guardado.getIdUsuario(),
                    "Motivo: " + guardado.getMotivoRechazo()
            );

            servicioNotificaciones.notificar(
                    guardado.getIdUsuario(),
                    "Verificación rechazada",
                    "Tu documentación fue rechazada: " + guardado.getMotivoRechazo() + ". Puedes volver a cargar tu documento.",
                    "VERIFICACION_RECHAZADA"
            );

            return new ResultadoVerificacionDTO(
                    guardado.getIdUsuario(),
                    guardado.getNombre(),
                    guardado.getEstadoVerificacion(),
                    guardado.getMotivoRechazo(),
                    guardado.getFechaVerificacion(),
                    guardado.isDisponible()
            );
        };
        return comando.ejecutar();
    }

    private Profesional buscarProfesional(Long idProfesional) {
        return profesionalRepositorio.findById(idProfesional)
                .orElseThrow(() -> new ExcepcionRecursoNoEncontrado("Profesional no encontrado."));
    }

    private Administrador buscarAdmin(Long idAdmin) {
        return administradorRepositorio.findById(idAdmin)
                .orElseThrow(() -> new ExcepcionRecursoNoEncontrado("Administrador no encontrado."));
    }
}
