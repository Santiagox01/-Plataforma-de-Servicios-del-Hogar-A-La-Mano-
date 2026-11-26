package co.edu.udea.alamano.comun.almacenamiento;

import co.edu.udea.alamano.comun.configuracion.PropiedadesApp;
import co.edu.udea.alamano.comun.excepcion.ExcepcionNegocio;
import co.edu.udea.alamano.comun.excepcion.ExcepcionNoAutorizado;
import co.edu.udea.alamano.comun.excepcion.ExcepcionRecursoNoEncontrado;
import co.edu.udea.alamano.comun.validacion.ValidadorDatosComunes;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Base64;
import java.util.Locale;
import java.util.UUID;

@Component
@ConditionalOnProperty(name = "app.storage.tipo", havingValue = "local", matchIfMissing = true)
public class LocalDiskStorageAdapter implements IAlmacenamientoArchivos {

    private static final String ALGORITMO_HMAC = "HmacSHA256";
    private static final byte[] PDF_SEMILLA_MINIMO = ("%PDF-1.4\n1 0 obj<< /Type /Catalog /Pages 2 0 R >>endobj\n"
            + "2 0 obj<< /Type /Pages /Kids [3 0 R] /Count 1 >>endobj\n"
            + "3 0 obj<< /Type /Page /Parent 2 0 R /MediaBox [0 0 200 200] >>endobj\n"
            + "xref\n0 4\n0000000000 65535 f \n0000000009 00000 n \n0000000058 00000 n \n0000000115 00000 n \n"
            + "trailer<< /Size 4 /Root 1 0 R >>\nstartxref\n190\n%%EOF\n").getBytes(StandardCharsets.US_ASCII);

    private final PropiedadesApp propiedades;
    private final ValidadorDatosComunes validador;
    private final Path directorioRaiz;

    public LocalDiskStorageAdapter(PropiedadesApp propiedades, ValidadorDatosComunes validador) {
        this.propiedades = propiedades;
        this.validador = validador;
        this.directorioRaiz = Paths.get(propiedades.getStorage().getLocalDir()).toAbsolutePath().normalize();
    }

    @Override
    public String guardarArchivoPrivado(MultipartFile archivo, String carpeta) {
        validador.validarArchivoDocumento(archivo);
        String extension = validador.extraerExtension(archivo.getOriginalFilename());
        String carpetaLimpia = sanitizarCarpeta(carpeta);
        String nombreUnico = UUID.randomUUID() + "." + extension;
        String rutaRelativa = carpetaLimpia.isEmpty() ? nombreUnico : carpetaLimpia + "/" + nombreUnico;

        Path destino = directorioRaiz.resolve(rutaRelativa).normalize();
        if (!destino.startsWith(directorioRaiz)) {
            throw new ExcepcionNegocio("Ruta de almacenamiento inválida.");
        }

        try {
            Files.createDirectories(destino.getParent());
            Files.write(destino, archivo.getBytes());
            return rutaRelativa;
        } catch (IOException e) {
            throw new ExcepcionNegocio("No fue posible guardar el archivo en almacenamiento privado.");
        }
    }

    @Override
    public String generarUrlFirmadaTemporal(String rutaInterna) {
        if (rutaInterna == null || rutaInterna.isBlank()) {
            throw new ExcepcionRecursoNoEncontrado("No existe archivo registrado.");
        }
        long expiraEn = Instant.now().getEpochSecond() + propiedades.getStorage().getExpiracionUrlSegundos();
        String carga = rutaInterna + "|" + expiraEn;
        String cargaB64 = Base64.getUrlEncoder().withoutPadding().encodeToString(carga.getBytes(StandardCharsets.UTF_8));
        String firmaB64 = firmarHmac(cargaB64);
        String baseUrl = propiedades.getPublicBaseUrl().replaceAll("/+$", "");
        return baseUrl + "/api/archivos/privado/" + cargaB64 + "." + firmaB64;
    }

    @Override
    public RecursoArchivo obtenerArchivoPorToken(String token) {
        if (token == null || !token.contains(".")) {
            throw new ExcepcionNoAutorizado("Token de archivo inválido.");
        }
        String[] partes = token.split("\\.", 2);
        String cargaB64 = partes[0];
        String firmaRecibida = partes[1];
        String firmaEsperada = firmarHmac(cargaB64);

        if (!MessageDigest.isEqual(
                firmaRecibida.getBytes(StandardCharsets.UTF_8),
                firmaEsperada.getBytes(StandardCharsets.UTF_8))) {
            throw new ExcepcionNoAutorizado("Firma de acceso al documento inválida.");
        }

        String carga = new String(Base64.getUrlDecoder().decode(cargaB64), StandardCharsets.UTF_8);
        int separador = carga.lastIndexOf('|');
        if (separador <= 0) {
            throw new ExcepcionNoAutorizado("Token de documento malformado.");
        }

        String rutaInterna = carga.substring(0, separador);
        long expiraEn;
        try {
            expiraEn = Long.parseLong(carga.substring(separador + 1));
        } catch (NumberFormatException e) {
            throw new ExcepcionNoAutorizado("Expiración de token inválida.");
        }

        if (Instant.now().getEpochSecond() > expiraEn) {
            throw new ExcepcionNoAutorizado("El enlace temporal para visualizar el documento ha expirado.");
        }

        Path rutaArchivo = directorioRaiz.resolve(rutaInterna).normalize();
        if (!rutaArchivo.startsWith(directorioRaiz)) {
            throw new ExcepcionNoAutorizado("Acceso denegado fuera del directorio privado.");
        }

        if (!Files.exists(rutaArchivo)) {
            if (rutaInterna.startsWith("seed/")) {
                return new RecursoArchivo(PDF_SEMILLA_MINIMO, "application/pdf", Paths.get(rutaInterna).getFileName().toString());
            }
            throw new ExcepcionRecursoNoEncontrado("Documento no encontrado en almacenamiento.");
        }

        try {
            byte[] contenido = Files.readAllBytes(rutaArchivo);
            String nombre = rutaArchivo.getFileName().toString();
            return new RecursoArchivo(contenido, resolverTipoContenido(nombre), nombre);
        } catch (IOException e) {
            throw new ExcepcionNegocio("Error al leer el documento privado.");
        }
    }

    private String firmarHmac(String datos) {
        try {
            String secreto = propiedades.getStorage().getFirmaSecret();
            if (secreto == null || secreto.isBlank()) {
                secreto = propiedades.getJwt().getSecret();
            }
            Mac mac = Mac.getInstance(ALGORITMO_HMAC);
            mac.init(new SecretKeySpec(secreto.getBytes(StandardCharsets.UTF_8), ALGORITMO_HMAC));
            byte[] hash = mac.doFinal(datos.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
        } catch (Exception e) {
            throw new IllegalStateException("Error generando firma HMAC para almacenamiento privado.", e);
        }
    }

    private String sanitizarCarpeta(String carpeta) {
        if (carpeta == null) {
            return "";
        }
        return carpeta.replace("..", "").replace("\\", "/").replaceAll("^/+|/+$", "");
    }

    private String resolverTipoContenido(String nombreArchivo) {
        String ext = validador.extraerExtension(nombreArchivo).toLowerCase(Locale.ROOT);
        return switch (ext) {
            case "pdf" -> "application/pdf";
            case "png" -> "image/png";
            case "jpg", "jpeg" -> "image/jpeg";
            default -> "application/octet-stream";
        };
    }
}
