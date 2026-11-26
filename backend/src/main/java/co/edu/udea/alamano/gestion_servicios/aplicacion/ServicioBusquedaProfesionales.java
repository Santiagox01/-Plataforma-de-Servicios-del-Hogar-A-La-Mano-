package co.edu.udea.alamano.gestion_servicios.aplicacion;

import co.edu.udea.alamano.autenticacion_seguridad.dominio.EstadoVerificacion;
import co.edu.udea.alamano.autenticacion_seguridad.dominio.Profesional;
import co.edu.udea.alamano.autenticacion_seguridad.infraestructura.ProfesionalRepositorio;
import co.edu.udea.alamano.autenticacion_seguridad.infraestructura.RepositorioFactory;
import co.edu.udea.alamano.comun.configuracion.TarifaConfig;
import co.edu.udea.alamano.comun.excepcion.ExcepcionNegocio;
import co.edu.udea.alamano.comun.excepcion.ExcepcionRecursoNoEncontrado;
import co.edu.udea.alamano.comun.validacion.ValidadorDatosComunes;
import co.edu.udea.alamano.cotizacion_tarificacion.dominio.Servicio;
import co.edu.udea.alamano.cotizacion_tarificacion.infraestructura.CategoriaOficioRepositorio;
import co.edu.udea.alamano.gestion_servicios.dto.CategoriaDTO;
import co.edu.udea.alamano.gestion_servicios.dto.PerfilProfesionalPublicoDTO;
import co.edu.udea.alamano.gestion_servicios.dto.ProfesionalResumenDTO;
import co.edu.udea.alamano.gestion_servicios.dto.ResultadoBusquedaProfesionalesDTO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

@Service
public class ServicioBusquedaProfesionales {

    private final CategoriaOficioRepositorio categoriaOficioRepositorio;
    private final ProfesionalRepositorio profesionalRepositorio;
    private final CalculadorDistanciaHaversine calculadorDistancia;
    private final IConsultaReputacion consultaReputacion;
    private final ValidadorDatosComunes validador;
    private final TarifaConfig tarifaConfig;

    public ServicioBusquedaProfesionales(CategoriaOficioRepositorio categoriaOficioRepositorio,
                                         RepositorioFactory repositorioFactory,
                                         CalculadorDistanciaHaversine calculadorDistancia,
                                         IConsultaReputacion consultaReputacion,
                                         ValidadorDatosComunes validador,
                                         TarifaConfig tarifaConfig) {
        this.categoriaOficioRepositorio = categoriaOficioRepositorio;
        this.profesionalRepositorio = repositorioFactory.crearProfesionalRepositorio();
        this.calculadorDistancia = calculadorDistancia;
        this.consultaReputacion = consultaReputacion;
        this.validador = validador;
        this.tarifaConfig = tarifaConfig;
    }

    @Transactional(readOnly = true)
    public List<CategoriaDTO> listarCategorias() {
        return categoriaOficioRepositorio.findAllByOrderByNombreAsc()
                .stream()
                .map(c -> new CategoriaDTO(c.getIdCategoria(), c.getNombre(), c.getDescripcion()))
                .toList();
    }

    @Transactional(readOnly = true)
    public ResultadoBusquedaProfesionalesDTO buscarProfesionales(Long categoriaId,
                                                                 Double latitud,
                                                                 Double longitud,
                                                                 Double radioKm) {
        validador.validarCoordenadas(latitud, longitud);
        if (!categoriaOficioRepositorio.existsById(categoriaId)) {
            throw new ExcepcionRecursoNoEncontrado("La categoría seleccionada no existe.");
        }

        double radioAplicado = radioKm != null ? radioKm : tarifaConfig.getRadioBusquedaDefectoKm();
        if (radioAplicado <= 0) {
            throw new ExcepcionNegocio("El radio de búsqueda debe ser mayor a cero kilómetros.");
        }

        List<ProfesionalResumenDTO> resultados = profesionalRepositorio.buscarElegiblesPorCategoria(categoriaId)
                .stream()
                .filter(p -> p.esVisibleEnBusqueda() && p.getLatitud() != null && p.getLongitud() != null)
                .map(p -> mapearResumen(p, categoriaId, latitud, longitud))
                .filter(r -> r.distanciaKm() <= radioAplicado)
                .sorted(Comparator.comparingDouble(ProfesionalResumenDTO::distanciaKm)
                        .thenComparing(Comparator.comparingDouble(ProfesionalResumenDTO::calificacionPromedio).reversed()))
                .toList();

        String sugerencia = resultados.isEmpty()
                ? "No hay profesionales verificados disponibles en un radio de " + radioAplicado + " km. Te sugerimos ampliar el radio de búsqueda."
                : null;

        return new ResultadoBusquedaProfesionalesDTO(resultados, resultados.size(), radioAplicado, sugerencia);
    }

    @Transactional(readOnly = true)
    public PerfilProfesionalPublicoDTO consultarPerfilPublico(Long idProfesional) {
        Profesional profesional = profesionalRepositorio.buscarPorIdConServicios(idProfesional)
                .orElseThrow(() -> new ExcepcionRecursoNoEncontrado("Profesional no encontrado."));

        if (!profesional.estaActiva() || profesional.getEstadoVerificacion() != EstadoVerificacion.VERIFICADO) {
            throw new ExcepcionRecursoNoEncontrado("Profesional verificado no encontrado.");
        }

        IConsultaReputacion.ResumenReputacion reputacion = consultaReputacion.obtenerResumen(profesional.getIdUsuario());
        List<PerfilProfesionalPublicoDTO.ServicioOfrecidoDTO> servicios = profesional.getServicios()
                .stream()
                .map(s -> new PerfilProfesionalPublicoDTO.ServicioOfrecidoDTO(
                        s.getIdServicio(),
                        s.getCategoriaOficio().getIdCategoria(),
                        s.getCategoriaOficio().getNombre(),
                        s.getTarifaBase(),
                        s.getPrecioMin(),
                        s.getPrecioMax()
                ))
                .toList();

        return new PerfilProfesionalPublicoDTO(
                profesional.getIdUsuario(),
                profesional.getNombre(),
                profesional.getFotoPerfil(),
                profesional.getUbicacion(),
                true,
                "Verificado",
                profesional.isDisponible(),
                reputacion.calificacionPromedio(),
                reputacion.totalResenas(),
                servicios,
                reputacion.ultimosComentarios()
        );
    }

    private ProfesionalResumenDTO mapearResumen(Profesional p, Long categoriaId, double latCliente, double lonCliente) {
        double distancia = calculadorDistancia.calcularKm(latCliente, lonCliente, p.getLatitud(), p.getLongitud());
        IConsultaReputacion.ResumenReputacion reputacion = consultaReputacion.obtenerResumen(p.getIdUsuario());
        Servicio servicio = p.getServicios().stream()
                .filter(s -> s.getCategoriaOficio().getIdCategoria().equals(categoriaId))
                .findFirst()
                .orElse(null);

        return new ProfesionalResumenDTO(
                p.getIdUsuario(),
                p.getNombre(),
                p.getUbicacion(),
                distancia,
                reputacion.calificacionPromedio(),
                reputacion.totalResenas(),
                true,
                "Verificado",
                servicio != null ? servicio.getIdServicio() : null,
                servicio != null ? servicio.getTarifaBase() : null,
                servicio != null ? servicio.getPrecioMin() : null,
                servicio != null ? servicio.getPrecioMax() : null
        );
    }
}
