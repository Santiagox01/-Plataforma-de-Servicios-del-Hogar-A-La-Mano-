package co.edu.udea.alamano.comun.excepcion;

public class ExcepcionNegocio extends RuntimeException {
    public ExcepcionNegocio(String mensaje) {
        super(mensaje);
    }
}
