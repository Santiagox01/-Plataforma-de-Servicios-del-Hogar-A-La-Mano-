package co.edu.udea.alamano.comun.excepcion;

public class ExcepcionCuentaBloqueada extends RuntimeException {
    public ExcepcionCuentaBloqueada(String mensaje) {
        super(mensaje);
    }
}
