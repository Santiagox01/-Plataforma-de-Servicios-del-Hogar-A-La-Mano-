package co.edu.udea.alamano.comun.almacenamiento;

import co.edu.udea.alamano.comun.configuracion.PropiedadesApp;
import co.edu.udea.alamano.comun.excepcion.ExcepcionNegocio;
import co.edu.udea.alamano.comun.validacion.ValidadorDatosComunes;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;

/**
 * Adaptador listo para Supabase Storage privado (bucket no publico + URLs firmadas).
 * Se activa con STORAGE_TIPO=supabase y las credenciales en .env.
 */
@Component
@ConditionalOnProperty(name = "app.storage.tipo", havingValue = "supabase")
public class SupabaseStorageAdapter implements IAlmacenamientoArchivos {

    private final PropiedadesApp propiedades;
    private final ValidadorDatosComunes validador;
    private final RestClient restClient;

    public SupabaseStorageAdapter(PropiedadesApp propiedades, ValidadorDatosComunes validador) {
        this.propiedades = propiedades;
        this.validador = validador;
        String baseUrl = propiedades.getStorage().getSupabaseUrl();
        if (baseUrl == null || baseUrl.isBlank()) {
            throw new IllegalStateException("SUPABASE_STORAGE_URL es obligatorio cuando STORAGE_TIPO=supabase.");
        }
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl.replaceAll("/+$", ""))
                .defaultHeader("Authorization", "Bearer " + propiedades.getStorage().getSupabaseServiceRoleKey())
                .build();
    }

    @Override
    public String guardarArchivoPrivado(MultipartFile archivo, String carpeta) {
        validador.validarArchivoDocumento(archivo);
        String extension = validador.extraerExtension(archivo.getOriginalFilename());
        String carpetaLimpia = carpeta == null ? "documentos" : carpeta.replace("..", "").replaceAll("^/+|/+$", "");
        String rutaObjeto = carpetaLimpia + "/" + UUID.randomUUID() + "." + extension;
        String bucket = propiedades.getStorage().getSupabaseBucket();

        try {
            MediaType mediaType = archivo.getContentType() != null
                    ? MediaType.parseMediaType(archivo.getContentType())
                    : MediaType.APPLICATION_OCTET_STREAM;

            restClient.post()
                    .uri("/storage/v1/object/" + bucket + "/" + rutaObjeto)
                    .contentType(mediaType)
                    .body(archivo.getBytes())
                    .retrieve()
                    .toBodilessEntity();

            return rutaObjeto;
        } catch (IOException e) {
            throw new ExcepcionNegocio("No fue posible leer el archivo para subirlo a Supabase Storage.");
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public String generarUrlFirmadaTemporal(String rutaInterna) {
        String bucket = propiedades.getStorage().getSupabaseBucket();
        long expiracion = propiedades.getStorage().getExpiracionUrlSegundos();

        Map<String, Object> respuesta = restClient.post()
                .uri("/storage/v1/object/sign/" + bucket + "/" + rutaInterna)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("expiresIn", expiracion))
                .retrieve()
                .body(Map.class);

        if (respuesta == null || !respuesta.containsKey("signedURL")) {
            throw new ExcepcionNegocio("No fue posible generar la URL firmada en Supabase Storage.");
        }
        String signedPath = String.valueOf(respuesta.get("signedURL"));
        String baseUrl = propiedades.getStorage().getSupabaseUrl().replaceAll("/+$", "");
        return signedPath.startsWith("http") ? signedPath : baseUrl + "/storage/v1" + signedPath;
    }

    @Override
    public RecursoArchivo obtenerArchivoPorToken(String token) {
        throw new UnsupportedOperationException("En modo Supabase Storage la descarga inline ocurre directamente mediante la URL firmada.");
    }
}
