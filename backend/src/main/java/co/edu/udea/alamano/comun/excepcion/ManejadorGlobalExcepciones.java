package co.edu.udea.alamano.comun.excepcion;

import jakarta.persistence.OptimisticLockException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;

import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class ManejadorGlobalExcepciones {

    @ExceptionHandler(ExcepcionNegocio.class)
    public ResponseEntity<RespuestaError> manejarNegocio(ExcepcionNegocio ex, HttpServletRequest req) {
        return construir(HttpStatus.BAD_REQUEST, ex.getMessage(), req, null);
    }

    @ExceptionHandler(ExcepcionRecursoNoEncontrado.class)
    public ResponseEntity<RespuestaError> manejarNoEncontrado(ExcepcionRecursoNoEncontrado ex, HttpServletRequest req) {
        return construir(HttpStatus.NOT_FOUND, ex.getMessage(), req, null);
    }

    @ExceptionHandler(ExcepcionNoAutorizado.class)
    public ResponseEntity<RespuestaError> manejarNoAutorizado(ExcepcionNoAutorizado ex, HttpServletRequest req) {
        return construir(HttpStatus.UNAUTHORIZED, ex.getMessage(), req, null);
    }

    @ExceptionHandler(ExcepcionConflicto.class)
    public ResponseEntity<RespuestaError> manejarConflicto(ExcepcionConflicto ex, HttpServletRequest req) {
        return construir(HttpStatus.CONFLICT, ex.getMessage(), req, null);
    }

    @ExceptionHandler({
            ObjectOptimisticLockingFailureException.class,
            OptimisticLockException.class,
            ConcurrencyFailureException.class,
            DataIntegrityViolationException.class
    })
    public ResponseEntity<RespuestaError> manejarConflictoConcurrencia(Exception ex, HttpServletRequest req) {
        return construir(HttpStatus.CONFLICT,
                "La solicitud fue modificada simultáneamente o ya existe un registro duplicado.",
                req, null);
    }

    @ExceptionHandler(ExcepcionCuentaBloqueada.class)
    public ResponseEntity<RespuestaError> manejarCuentaBloqueada(ExcepcionCuentaBloqueada ex, HttpServletRequest req) {
        return construir(HttpStatus.LOCKED, ex.getMessage(), req, null);
    }

    @ExceptionHandler(ExcepcionCuentaRestringida.class)
    public ResponseEntity<RespuestaError> manejarCuentaRestringida(ExcepcionCuentaRestringida ex, HttpServletRequest req) {
        return construir(HttpStatus.FORBIDDEN, ex.getMessage(), req, null);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<RespuestaError> manejarAccesoDenegado(AccessDeniedException ex, HttpServletRequest req) {
        return construir(HttpStatus.FORBIDDEN, "No tienes permisos para realizar esta acción.", req, null);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<RespuestaError> manejarValidacion(MethodArgumentNotValidException ex, HttpServletRequest req) {
        Map<String, String> detalles = new LinkedHashMap<>();
        for (FieldError err : ex.getBindingResult().getFieldErrors()) {
            detalles.putIfAbsent(err.getField(), err.getDefaultMessage());
        }
        return construir(HttpStatus.BAD_REQUEST, "Datos de entrada inválidos. Revisa los campos requeridos.", req, detalles);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<RespuestaError> manejarViolacionRestriccion(ConstraintViolationException ex, HttpServletRequest req) {
        return construir(HttpStatus.BAD_REQUEST, "Parámetros inválidos: " + ex.getMessage(), req, null);
    }

    @ExceptionHandler(MissingServletRequestPartException.class)
    public ResponseEntity<RespuestaError> manejarParteFaltante(MissingServletRequestPartException ex, HttpServletRequest req) {
        return construir(HttpStatus.BAD_REQUEST, "Falta el archivo requerido: " + ex.getRequestPartName(), req, null);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<RespuestaError> manejarParametroFaltante(MissingServletRequestParameterException ex, HttpServletRequest req) {
        return construir(HttpStatus.BAD_REQUEST, "Falta el parámetro obligatorio: " + ex.getParameterName(), req, null);
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<RespuestaError> manejarTamanoExcedido(MaxUploadSizeExceededException ex, HttpServletRequest req) {
        return construir(HttpStatus.BAD_REQUEST, "El archivo excede el tamaño máximo permitido de 10 MB.", req, null);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<RespuestaError> manejarArgumentoInvalido(IllegalArgumentException ex, HttpServletRequest req) {
        return construir(HttpStatus.BAD_REQUEST, ex.getMessage(), req, null);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<RespuestaError> manejarGeneral(Exception ex, HttpServletRequest req) {
        return construir(HttpStatus.INTERNAL_SERVER_ERROR, "Ocurrió un error interno procesando la solicitud.", req, null);
    }

    private ResponseEntity<RespuestaError> construir(HttpStatus estado, String mensaje, HttpServletRequest req, Map<String, String> detalles) {
        RespuestaError cuerpo = new RespuestaError(
                OffsetDateTime.now(),
                estado.value(),
                estado.getReasonPhrase(),
                mensaje,
                req.getRequestURI(),
                detalles
        );
        return ResponseEntity.status(estado).body(cuerpo);
    }
}
