package co.edu.udea.alamano.comun.almacenamiento;

public record RecursoArchivo(
        byte[] contenido,
        String tipoContenido,
        String nombreArchivo
) {
}
