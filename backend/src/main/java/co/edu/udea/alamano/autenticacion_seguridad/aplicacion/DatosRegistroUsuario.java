package co.edu.udea.alamano.autenticacion_seguridad.aplicacion;

import co.edu.udea.alamano.cotizacion_tarificacion.dominio.CategoriaOficio;

import java.util.List;

public record DatosRegistroUsuario(
        String nombre,
        String correo,
        String celular,
        String contrasena,
        String ubicacion,
        Double latitud,
        Double longitud,
        String rutaDocumentoIdentidad,
        List<String> rutasCertificaciones,
        List<CategoriaOficio> categorias
) {
}
