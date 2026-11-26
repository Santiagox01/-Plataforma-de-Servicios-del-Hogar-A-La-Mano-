package co.edu.udea.alamano.pagos.infraestructura;

import co.edu.udea.alamano.pagos.dominio.MetodoPago;
import co.edu.udea.alamano.pagos.dominio.TipoMetodoPago;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MetodoPagoRepositorio extends JpaRepository<MetodoPago, Long> {

    List<MetodoPago> findByClienteIdUsuarioAndActivoTrueOrderByFechaRegistroDesc(Long idCliente);

    Optional<MetodoPago> findByIdMetodoPagoAndClienteIdUsuarioAndActivoTrue(Long idMetodoPago, Long idCliente);

    Optional<MetodoPago> findFirstByClienteIdUsuarioAndTipoAndActivoTrue(Long idCliente, TipoMetodoPago tipo);
}
