package co.edu.udea.alamano.comun.excepcion;

/**
 * Excepcion lanzada cuando una cuenta esta suspendida temporalmente o desactivada.
 */
public class ExcepcionCuentaRestringida extends RuntimeException {
    public ExcepcionCuentaRestringida(String message) {
        super(message);
    }
}
