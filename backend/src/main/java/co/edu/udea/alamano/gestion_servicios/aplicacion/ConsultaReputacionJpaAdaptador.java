package co.edu.udea.alamano.gestion_servicios.aplicacion;

import co.edu.udea.alamano.gestion_servicios.infraestructura.ResenaRepositorio;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
public class ConsultaReputacionJpaAdaptador implements IConsultaReputacion {

    private final ResenaRepositorio resenaRepositorio;

    public ConsultaReputacionJpaAdaptador(ResenaRepositorio resenaRepositorio) {
        this.resenaRepositorio = resenaRepositorio;
    }

    @Override
    @Transactional(readOnly = true)
    public ResumenReputacion obtenerResumen(Long idProfesional) {
        long total = resenaRepositorio.countByProfesionalIdUsuario(idProfesional);
        if (total == 0) {
            return new ResumenReputacion(0.0, 0, List.of());
        }

        long suma = resenaRepositorio.sumarEstrellasPorProfesional(idProfesional);
        double promedio = Math.round(((suma * 1.0) / total) * 100.0) / 100.0;

        List<ComentarioPublico> comentarios = resenaRepositorio
                .listarRecientesPorProfesional(idProfesional, PageRequest.of(0, 5))
                .stream()
                .map(r -> new ComentarioPublico(
                        r.getCliente().getNombre(),
                        r.getEstrellas(),
                        r.getComentario(),
                        r.getFechaResena().toString()
                ))
                .toList();

        return new ResumenReputacion(promedio, (int) total, comentarios);
    }
}
