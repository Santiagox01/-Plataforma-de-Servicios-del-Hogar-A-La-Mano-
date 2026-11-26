package co.edu.udea.alamano.comun.auditoria;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AuditoriaAdministrativaRepositorio extends JpaRepository<AuditoriaAdministrativa, Long> {
    List<AuditoriaAdministrativa> findByEntidadAfectadaAndIdEntidadOrderByFechaAccionDesc(String entidadAfectada, Long idEntidad);
}
