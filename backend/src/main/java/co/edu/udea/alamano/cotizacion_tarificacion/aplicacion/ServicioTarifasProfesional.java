package co.edu.udea.alamano.cotizacion_tarificacion.aplicacion;

import co.edu.udea.alamano.autenticacion_seguridad.dominio.Profesional;
import co.edu.udea.alamano.autenticacion_seguridad.infraestructura.ProfesionalRepositorio;
import co.edu.udea.alamano.autenticacion_seguridad.infraestructura.RepositorioFactory;
import co.edu.udea.alamano.comun.excepcion.ExcepcionRecursoNoEncontrado;
import co.edu.udea.alamano.cotizacion_tarificacion.dominio.Servicio;
import co.edu.udea.alamano.cotizacion_tarificacion.dto.ConfigurarTarifaDTO;
import co.edu.udea.alamano.cotizacion_tarificacion.dto.ServicioTarifaDTO;
import co.edu.udea.alamano.cotizacion_tarificacion.infraestructura.ServicioRepositorio;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ServicioTarifasProfesional {

    private final ServicioRepositorio servicioRepositorio;
    private final ProfesionalRepositorio profesionalRepositorio;

    public ServicioTarifasProfesional(ServicioRepositorio servicioRepositorio,
                                      RepositorioFactory repositorioFactory) {
        this.servicioRepositorio = servicioRepositorio;
        this.profesionalRepositorio = repositorioFactory.crearProfesionalRepositorio();
    }

    @Transactional(readOnly = true)
    public List<ServicioTarifaDTO> listarMisServicios(Long idProfesional) {
        return servicioRepositorio.findByProfesionalIdUsuario(idProfesional)
                .stream()
                .map(this::mapearDTO)
                .toList();
    }

    @Transactional
    public ServicioTarifaDTO configurarTarifa(Long idProfesional, ConfigurarTarifaDTO dto) {
        Profesional profesional = profesionalRepositorio.findById(idProfesional)
                .orElseThrow(() -> new ExcepcionRecursoNoEncontrado("Profesional no encontrado."));

        Servicio servicio = servicioRepositorio
                .findByProfesionalIdUsuarioAndCategoriaOficioIdCategoria(profesional.getIdUsuario(), dto.idCategoria())
                .orElseThrow(() -> new ExcepcionRecursoNoEncontrado("No tienes un servicio registrado en esa categoría."));

        servicio.configurarTarifa(dto.tarifaBase(), dto.precioMin(), dto.precioMax());
        Servicio guardado = servicioRepositorio.save(servicio);
        return mapearDTO(guardado);
    }

    private ServicioTarifaDTO mapearDTO(Servicio s) {
        return new ServicioTarifaDTO(
                s.getIdServicio(),
                s.getCategoriaOficio().getIdCategoria(),
                s.getCategoriaOficio().getNombre(),
                s.getTarifaBase(),
                s.getPrecioMin(),
                s.getPrecioMax(),
                s.tieneTarifaConfigurada()
        );
    }
}
