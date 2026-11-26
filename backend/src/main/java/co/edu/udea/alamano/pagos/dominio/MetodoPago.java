package co.edu.udea.alamano.pagos.dominio;

import co.edu.udea.alamano.autenticacion_seguridad.dominio.Cliente;
import co.edu.udea.alamano.comun.excepcion.ExcepcionNegocio;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;
import java.time.YearMonth;
import java.util.UUID;

/**
 * Metodo de pago del cliente; para tarjetas almacena solo token, tipo y ultimos 4 digitos,
 * y para PayPal vincula el tipo PAYPAL sin almacenar credenciales ni tarjetas.
 */
@Entity
@Table(name = "metodos_pago")
public class MetodoPago {

    private static final String SUFIJO_PAYPAL = "PPAL";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_metodo_pago")
    private Long idMetodoPago;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_cliente", nullable = false)
    private Cliente cliente;

    @Column(name = "token", nullable = false, unique = true, length = 160)
    private String token;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", nullable = false, length = 30)
    private TipoMetodoPago tipo;

    @Enumerated(EnumType.STRING)
    @Column(name = "franquicia", nullable = false, length = 30)
    private FranquiciaTarjeta franquicia;

    @Column(name = "ultimos_digitos", nullable = false, length = 4)
    private String ultimosDigitos;

    @Column(name = "titular", nullable = false, length = 120)
    private String titular;

    @Column(name = "mes_vencimiento")
    private Integer mesVencimiento;

    @Column(name = "anio_vencimiento")
    private Integer anioVencimiento;

    @Column(name = "activo", nullable = false)
    private boolean activo = true;

    @Column(name = "fecha_registro", nullable = false)
    private OffsetDateTime fechaRegistro;

    protected MetodoPago() {
    }

    public MetodoPago(Cliente cliente,
                      String token,
                      TipoMetodoPago tipo,
                      FranquiciaTarjeta franquicia,
                      String ultimosDigitos,
                      String titular,
                      Integer mesVencimiento,
                      Integer anioVencimiento) {
        if (cliente == null || token == null || token.isBlank()) {
            throw new ExcepcionNegocio("No es posible registrar un método de pago sin cliente ni token válido.");
        }
        if (ultimosDigitos == null || ultimosDigitos.length() != 4) {
            throw new ExcepcionNegocio("Los últimos dígitos del método de pago son obligatorios.");
        }
        this.cliente = cliente;
        this.token = token;
        this.tipo = tipo;
        this.franquicia = franquicia != null ? franquicia : FranquiciaTarjeta.OTRA;
        this.ultimosDigitos = ultimosDigitos;
        this.titular = titular != null ? titular.trim() : "";
        this.mesVencimiento = mesVencimiento;
        this.anioVencimiento = anioVencimiento;
        this.activo = true;
        this.fechaRegistro = OffsetDateTime.now();
    }

    public static MetodoPago crearMetodoPayPal(Cliente cliente, String correoOAlias) {
        String alias = (correoOAlias != null && !correoOAlias.isBlank())
                ? correoOAlias.trim()
                : cliente.getCorreo();
        String sufijo = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        String tokenPayPal = "paypal_cli_" + cliente.getIdUsuario() + "_" + sufijo;
        return new MetodoPago(
                cliente,
                tokenPayPal,
                TipoMetodoPago.PAYPAL,
                FranquiciaTarjeta.PAYPAL,
                SUFIJO_PAYPAL,
                alias,
                null,
                null
        );
    }

    public void desactivar() {
        this.activo = false;
    }

    public boolean esDeCliente(Long idCliente) {
        return idCliente != null && cliente.getIdUsuario().equals(idCliente);
    }

    public boolean estaVencido(YearMonth actual) {
        if (this.tipo == TipoMetodoPago.PAYPAL || mesVencimiento == null || anioVencimiento == null) {
            return false;
        }
        return YearMonth.of(anioVencimiento, mesVencimiento).isBefore(actual);
    }

    public String resumenEnmascarado() {
        if (this.tipo == TipoMetodoPago.PAYPAL) {
            return (titular != null && !titular.isBlank()) ? "PayPal (" + titular + ")" : "PayPal";
        }
        return franquicia.name() + " •••• " + ultimosDigitos;
    }

    public Long getIdMetodoPago() {
        return idMetodoPago;
    }

    public Cliente getCliente() {
        return cliente;
    }

    public String getToken() {
        return token;
    }

    public TipoMetodoPago getTipo() {
        return tipo;
    }

    public FranquiciaTarjeta getFranquicia() {
        return franquicia;
    }

    public String getUltimosDigitos() {
        return ultimosDigitos;
    }

    public String getTitular() {
        return titular;
    }

    public Integer getMesVencimiento() {
        return mesVencimiento;
    }

    public Integer getAnioVencimiento() {
        return anioVencimiento;
    }

    public boolean isActivo() {
        return activo;
    }

    public OffsetDateTime getFechaRegistro() {
        return fechaRegistro;
    }
}
