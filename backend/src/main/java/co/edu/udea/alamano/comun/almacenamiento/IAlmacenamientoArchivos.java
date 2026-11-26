package co.edu.udea.alamano.comun.almacenamiento;

import org.springframework.web.multipart.MultipartFile;

/**
 * Puerto de almacenamiento privado (patron Variaciones Protegidas).
 * Los documentos sensibles nunca deben exponerse en rutas publicas permanentes.
 */
public interface IAlmacenamientoArchivos {

    String guardarArchivoPrivado(MultipartFile archivo, String carpeta);

    String generarUrlFirmadaTemporal(String rutaInterna);

    RecursoArchivo obtenerArchivoPorToken(String token);
}
