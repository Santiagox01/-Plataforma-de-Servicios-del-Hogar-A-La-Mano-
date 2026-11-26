package co.edu.udea.alamano.pagos.aplicacion;

import co.edu.udea.alamano.gestion_servicios.aplicacion.IConsultaPagoSolicitud;
import co.edu.udea.alamano.pagos.dominio.EstadoPago;
import co.edu.udea.alamano.pagos.dominio.Pago;
import co.edu.udea.alamano.pagos.infraestructura.PagoRepositorio;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Component
public class ConsultaPagoSolicitudAdaptador implements IConsultaPagoSolicitud {

    private static final List<EstadoPago> ESTADOS_CUSTODIA_CONFIRMADA = List.of(
            EstadoPago.RETENIDO,
            EstadoPago.LIBERADO,
            EstadoPago.REEMBOLSADO
    );

    private final PagoRepositorio pagoRepositorio;

    public ConsultaPagoSolicitudAdaptador(PagoRepositorio pagoRepositorio) {
        this.pagoRepositorio = pagoRepositorio;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ResumenPagoSolicitud> consultarPagoActual(Long idSolicitud) {
        Optional<Pago> pagoOpt = pagoRepositorio.buscarPagoActivoPorSolicitud(idSolicitud, ESTADOS_CUSTODIA_CONFIRMADA);
        if (pagoOpt.isEmpty()) {
            List<Pago> historial = pagoRepositorio.listarHistorialPorSolicitud(idSolicitud);
            if (!historial.isEmpty()) {
                pagoOpt = Optional.of(historial.get(0));
            }
        }
        return pagoOpt.map(p -> new ResumenPagoSolicitud(
                p.getIdPago(),
                p.getEstadoPago().name(),
                p.getMontoServicio(),
                p.getTasaGarantia(),
                p.getCantidad(),
                esEstadoConfirmado(p.getEstadoPago()) ? p.construirNumeroComprobante() : null,
                p.getEstadoPago() == EstadoPago.RETENIDO,
                !esEstadoConfirmado(p.getEstadoPago())
        ));
    }

    private boolean esEstadoConfirmado(EstadoPago estado) {
        return estado == EstadoPago.RETENIDO || estado == EstadoPago.LIBERADO || estado == EstadoPago.REEMBOLSADO;
    }
}
