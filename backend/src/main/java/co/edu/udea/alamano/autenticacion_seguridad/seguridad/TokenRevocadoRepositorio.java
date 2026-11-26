package co.edu.udea.alamano.autenticacion_seguridad.seguridad;

import org.springframework.data.jpa.repository.JpaRepository;

public interface TokenRevocadoRepositorio extends JpaRepository<TokenRevocado, String> {
    boolean existsByJti(String jti);
}
