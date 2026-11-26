package co.edu.udea.alamano.comun.excepcion;

public class ExcepcionNoAutorizado extends RuntimeException {
    public ExcepcionNoAutorizado(String mensaje) {
        super(mensaje);
    }
}
