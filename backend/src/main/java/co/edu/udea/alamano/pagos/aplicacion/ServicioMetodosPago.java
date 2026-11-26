package co.edu.udea.alamano.pagos.aplicacion;

import co.edu.udea.alamano.autenticacion_seguridad.dominio.Cliente;
import co.edu.udea.alamano.autenticacion_seguridad.infraestructura.ClienteRepositorio;
import co.edu.udea.alamano.autenticacion_seguridad.infraestructura.RepositorioFactory;
import co.edu.udea.alamano.comun.configuracion.TarifaConfig;
import co.edu.udea.alamano.comun.excepcion.ExcepcionNegocio;
import co.edu.udea.alamano.comun.excepcion.ExcepcionRecursoNoEncontrado;
import co.edu.udea.alamano.pagos.dominio.DatosTarjetaValidacion;
import co.edu.udea.alamano.pagos.dominio.IPasarelaPagos;
import co.edu.udea.alamano.pagos.dominio.MetodoPago;
import co.edu.udea.alamano.pagos.dominio.ResultadoValidacionMetodo;
import co.edu.udea.alamano.pagos.dominio.TipoMetodoPago;
import co.edu.udea.alamano.pagos.dto.ConfiguracionPagosDTO;
import co.edu.udea.alamano.pagos.dto.MetodoPagoDTO;
import co.edu.udea.alamano.pagos.dto.RegistrarMetodoPagoDTO;
import co.edu.udea.alamano.pagos.infraestructura.MetodoPagoRepositorio;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class ServicioMetodosPago {

    private static final String MONEDA_EXTERNA_USD = "USD";

    private final MetodoPagoRepositorio metodoPagoRepositorio;
    private final ClienteRepositorio clienteRepositorio;
    private final IPasarelaPagos pasarelaPagos;
    private final TarifaConfig tarifaConfig;

    public ServicioMetodosPago(MetodoPagoRepositorio metodoPagoRepositorio,
                               RepositorioFactory repositorioFactory,
                               IPasarelaPagos pasarelaPagos) {
        this(metodoPagoRepositorio, repositorioFactory, pasarelaPagos, new TarifaConfig());
    }

    @Autowired
    public ServicioMetodosPago(MetodoPagoRepositorio metodoPagoRepositorio,
                               RepositorioFactory repositorioFactory,
                               IPasarelaPagos pasarelaPagos,
                               TarifaConfig tarifaConfig) {
        this.metodoPagoRepositorio = metodoPagoRepositorio;
        this.clienteRepositorio = repositorioFactory.crearClienteRepositorio();
        this.pasarelaPagos = pasarelaPagos;
        this.tarifaConfig = tarifaConfig;
    }

    public ConfiguracionPagosDTO obtenerConfiguracionPagos() {
        return new ConfiguracionPagosDTO(
                pasarelaPagos.obtenerTipoPasarela(),
                pasarelaPagos.requiereAprobacionExterna(),
                MONEDA_EXTERNA_USD,
                tarifaConfig.getTasaCambioCopUsd()
        );
    }

    @Transactional
    public MetodoPagoDTO registrar(Long idCliente, RegistrarMetodoPagoDTO dto) {
        if (pasarelaPagos.requiereAprobacionExterna()) {
            throw new ExcepcionNegocio(
                    "Con la pasarela PayPal activa no se registran tarjetas; vincula PayPal o aprueba el pago directamente.");
        }
        Cliente cliente = clienteRepositorio.findById(idCliente)
                .orElseThrow(() -> new ExcepcionRecursoNoEncontrado("Cliente no encontrado."));

        DatosTarjetaValidacion datosValidacion = new DatosTarjetaValidacion(
                dto.numeroTarjeta(),
                dto.cvv(),
                dto.mesVencimiento(),
                dto.anioVencimiento(),
                dto.tipo(),
                dto.titular()
        );

        ResultadoValidacionMetodo validacion = pasarelaPagos.validarMetodo(datosValidacion);
        if (!validacion.valido()) {
            throw new ExcepcionNegocio(validacion.motivoRechazo() != null
                    ? validacion.motivoRechazo()
                    : "La pasarela rechazó el método de pago.");
        }

        // Nunca se persiste el numero completo de tarjeta ni el CVV: unicamente token, tipo y ultimos 4 digitos.
        MetodoPago metodo = new MetodoPago(
                cliente,
                validacion.token(),
                dto.tipo(),
                validacion.franquicia(),
                validacion.ultimosDigitos(),
                dto.titular(),
                dto.mesVencimiento(),
                dto.anioVencimiento()
        );

        return mapearDTO(metodoPagoRepositorio.save(metodo));
    }

    @Transactional
    public MetodoPagoDTO registrarPayPal(Long idCliente, String correoOAlias) {
        Cliente cliente = clienteRepositorio.findById(idCliente)
                .orElseThrow(() -> new ExcepcionRecursoNoEncontrado("Cliente no encontrado."));
        Optional<MetodoPago> existente = metodoPagoRepositorio
                .findFirstByClienteIdUsuarioAndTipoAndActivoTrue(idCliente, TipoMetodoPago.PAYPAL);
        if (existente.isPresent()) {
            return mapearDTO(existente.get());
        }
        MetodoPago nuevo = MetodoPago.crearMetodoPayPal(cliente, correoOAlias);
        return mapearDTO(metodoPagoRepositorio.save(nuevo));
    }

    @Transactional(readOnly = true)
    public List<MetodoPagoDTO> listarPorCliente(Long idCliente) {
        return metodoPagoRepositorio.findByClienteIdUsuarioAndActivoTrueOrderByFechaRegistroDesc(idCliente)
                .stream()
                .map(this::mapearDTO)
                .toList();
    }

    @Transactional
    public void eliminar(Long idCliente, Long idMetodoPago) {
        MetodoPago metodo = metodoPagoRepositorio.findByIdMetodoPagoAndClienteIdUsuarioAndActivoTrue(idMetodoPago, idCliente)
                .orElseThrow(() -> new ExcepcionRecursoNoEncontrado("Método de pago no encontrado."));
        metodo.desactivar();
        metodoPagoRepositorio.save(metodo);
    }

    private MetodoPagoDTO mapearDTO(MetodoPago m) {
        return new MetodoPagoDTO(
                m.getIdMetodoPago(),
                m.getToken(),
                m.getTipo(),
                m.getFranquicia(),
                m.getUltimosDigitos(),
                m.getTitular(),
                m.getMesVencimiento(),
                m.getAnioVencimiento(),
                m.resumenEnmascarado(),
                m.getFechaRegistro()
        );
    }
}
