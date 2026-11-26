package co.edu.udea.alamano.comun.auditoria;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ServicioAuditoria {

    private final AuditoriaAdministrativaRepositorio auditoriaRepositorio;

    public ServicioAuditoria(AuditoriaAdministrativaRepositorio auditoriaRepositorio) {
        this.auditoriaRepositorio = auditoriaRepositorio;
    }

    @Transactional
    public AuditoriaAdministrativa registrar(Long idAdministrador, String accion, String entidadAfectada, Long idEntidad, String detalle) {
        AuditoriaAdministrativa registro = new AuditoriaAdministrativa(idAdministrador, accion, entidadAfectada, idEntidad, detalle);
        return auditoriaRepositorio.save(registro);
    }
}
