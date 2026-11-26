package co.edu.udea.alamano.comun.validacion;

import co.edu.udea.alamano.comun.excepcion.ExcepcionNegocio;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Validador centralizado de reglas transversales (principio DRY).
 */
@Component
public class ValidadorDatosComunes {

    public static final long TAMANO_MAXIMO_ARCHIVO_BYTES = 10L * 1024L * 1024L;
    public static final int MAX_FOTOS_SOLICITUD = 3;
    private static final Pattern PATRON_CORREO = Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    private static final Pattern PATRON_CELULAR_CO = Pattern.compile("^3\\d{9}$");
    private static final Pattern PATRON_MAYUSCULA = Pattern.compile(".*[A-Z].*");
    private static final Pattern PATRON_NUMERO = Pattern.compile(".*\\d.*");

    private static final Set<String> EXTENSIONES_PERMITIDAS = Set.of("jpg", "jpeg", "png", "pdf");
    private static final Set<String> MIME_PERMITIDOS = Set.of("image/jpeg", "image/jpg", "image/png", "application/pdf");
    private static final Set<String> EXTENSIONES_IMAGEN = Set.of("jpg", "jpeg", "png");
    private static final Set<String> MIME_IMAGEN = Set.of("image/jpeg", "image/jpg", "image/png");

    public String normalizarYValidarCorreo(String correo) {
        if (correo == null || correo.isBlank()) {
            throw new ExcepcionNegocio("El correo electrónico es obligatorio.");
        }
        String limpio = correo.trim().toLowerCase(Locale.ROOT);
        if (!PATRON_CORREO.matcher(limpio).matches()) {
            throw new ExcepcionNegocio("El formato del correo electrónico no es válido.");
        }
        return limpio;
    }

    public String validarCelularColombiano(String celular) {
        if (celular == null || celular.isBlank()) {
            throw new ExcepcionNegocio("El número celular es obligatorio.");
        }
        String limpio = celular.trim();
        if (!PATRON_CELULAR_CO.matcher(limpio).matches()) {
            throw new ExcepcionNegocio("El celular debe ser un número colombiano de 10 dígitos que inicie en 3.");
        }
        return limpio;
    }

    public void validarContrasena(String contrasena) {
        if (contrasena == null || contrasena.length() < 8
                || !PATRON_MAYUSCULA.matcher(contrasena).matches()
                || !PATRON_NUMERO.matcher(contrasena).matches()) {
            throw new ExcepcionNegocio("La contraseña debe tener al menos 8 caracteres, una letra mayúscula y un número.");
        }
    }

    public void validarArchivoDocumento(MultipartFile archivo) {
        if (archivo == null || archivo.isEmpty()) {
            throw new ExcepcionNegocio("Debes adjuntar un documento válido (JPG, PNG o PDF, máximo 10 MB).");
        }
        if (archivo.getSize() > TAMANO_MAXIMO_ARCHIVO_BYTES) {
            throw new ExcepcionNegocio("El archivo excede el tamaño máximo permitido de 10 MB.");
        }

        String nombreOriginal = archivo.getOriginalFilename();
        String extension = extraerExtension(nombreOriginal);
        if (!EXTENSIONES_PERMITIDAS.contains(extension)) {
            throw new ExcepcionNegocio("Formato de archivo no soportado. Solo se permiten archivos JPG, PNG o PDF.");
        }

        String contentType = archivo.getContentType();
        if (contentType != null && !contentType.isBlank()) {
            String mimeLimpio = contentType.toLowerCase(Locale.ROOT).split(";")[0].trim();
            if (!MIME_PERMITIDOS.contains(mimeLimpio)) {
                throw new ExcepcionNegocio("Tipo de archivo no permitido. Solo se aceptan imágenes JPG, PNG o documentos PDF.");
            }
        }
    }

    public void validarFotoSolicitud(MultipartFile foto) {
        if (foto == null || foto.isEmpty()) {
            throw new ExcepcionNegocio("La fotografía adjunta no puede estar vacía.");
        }
        if (foto.getSize() > TAMANO_MAXIMO_ARCHIVO_BYTES) {
            throw new ExcepcionNegocio("Cada fotografía puede pesar máximo 10 MB.");
        }
        String extension = extraerExtension(foto.getOriginalFilename());
        if (!EXTENSIONES_IMAGEN.contains(extension)) {
            throw new ExcepcionNegocio("Formato de foto no soportado. Solo se permiten imágenes JPG o PNG.");
        }
        String contentType = foto.getContentType();
        if (contentType != null && !contentType.isBlank()) {
            String mimeLimpio = contentType.toLowerCase(Locale.ROOT).split(";")[0].trim();
            if (!MIME_IMAGEN.contains(mimeLimpio)) {
                throw new ExcepcionNegocio("Tipo de imagen no permitido. Solo se aceptan JPG o PNG.");
            }
        }
    }

    public void validarListaFotosSolicitud(List<MultipartFile> fotos) {
        if (fotos == null || fotos.isEmpty()) {
            return;
        }
        List<MultipartFile> noVacias = fotos.stream().filter(f -> f != null && !f.isEmpty()).toList();
        if (noVacias.size() > MAX_FOTOS_SOLICITUD) {
            throw new ExcepcionNegocio("Solo puedes adjuntar hasta " + MAX_FOTOS_SOLICITUD + " fotografías por solicitud.");
        }
        noVacias.forEach(this::validarFotoSolicitud);
    }

    public void validarCoordenadas(Double latitud, Double longitud) {
        if (latitud == null || longitud == null) {
            throw new ExcepcionNegocio("La latitud y longitud son obligatorias.");
        }
        if (latitud < -90.0 || latitud > 90.0 || longitud < -180.0 || longitud > 180.0) {
            throw new ExcepcionNegocio("Las coordenadas geográficas están fuera de rango.");
        }
    }

    public String extraerExtension(String nombreArchivo) {
        if (nombreArchivo == null) {
            return "";
        }
        int indice = nombreArchivo.lastIndexOf('.');
        if (indice < 0 || indice == nombreArchivo.length() - 1) {
            return "";
        }
        return nombreArchivo.substring(indice + 1).toLowerCase(Locale.ROOT);
    }
}
