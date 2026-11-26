package co.edu.udea.alamano.autenticacion_seguridad.aplicacion;

import co.edu.udea.alamano.autenticacion_seguridad.dominio.Cliente;
import co.edu.udea.alamano.autenticacion_seguridad.dominio.EstadoCuenta;
import co.edu.udea.alamano.autenticacion_seguridad.dominio.EstadoVerificacion;
import co.edu.udea.alamano.autenticacion_seguridad.dominio.Profesional;
import co.edu.udea.alamano.autenticacion_seguridad.dominio.Usuario;
import co.edu.udea.alamano.autenticacion_seguridad.dto.RespuestaAuthDTO;
import co.edu.udea.alamano.autenticacion_seguridad.dto.ResumenUsuarioDTO;
import co.edu.udea.alamano.autenticacion_seguridad.dto.SolicitudLoginDTO;
import co.edu.udea.alamano.autenticacion_seguridad.dto.SolicitudRegistroClienteDTO;
import co.edu.udea.alamano.autenticacion_seguridad.infraestructura.IUsuarioRepositorio;
import co.edu.udea.alamano.autenticacion_seguridad.infraestructura.RepositorioFactory;
import co.edu.udea.alamano.autenticacion_seguridad.seguridad.ServicioJwt;
import co.edu.udea.alamano.autenticacion_seguridad.seguridad.TokenRevocado;
import co.edu.udea.alamano.autenticacion_seguridad.seguridad.TokenRevocadoRepositorio;
import co.edu.udea.alamano.comun.almacenamiento.IAlmacenamientoArchivos;
import co.edu.udea.alamano.comun.configuracion.PropiedadesApp;
import co.edu.udea.alamano.comun.excepcion.ExcepcionConflicto;
import co.edu.udea.alamano.comun.excepcion.ExcepcionCuentaBloqueada;
import co.edu.udea.alamano.comun.excepcion.ExcepcionCuentaRestringida;
import co.edu.udea.alamano.comun.excepcion.ExcepcionNegocio;
import co.edu.udea.alamano.comun.excepcion.ExcepcionNoAutorizado;
import co.edu.udea.alamano.comun.excepcion.ExcepcionRecursoNoEncontrado;
import co.edu.udea.alamano.comun.validacion.ValidadorDatosComunes;
import co.edu.udea.alamano.cotizacion_tarificacion.dominio.CategoriaOficio;
import co.edu.udea.alamano.cotizacion_tarificacion.infraestructura.CategoriaOficioRepositorio;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class ServicioAutenticacion {

    private static final String ERROR_CREDENCIALES = "Correo o contraseña incorrectos.";
    private static final DateTimeFormatter FORMATO_FECHA_SUSPENSION = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final CreadorCliente creadorCliente;
    private final CreadorProfesional creadorProfesional;
    private final IUsuarioRepositorio usuarioRepositorio;
    private final CategoriaOficioRepositorio categoriaOficioRepositorio;
    private final IAlmacenamientoArchivos almacenamientoArchivos;
    private final ValidadorDatosComunes validador;
    private final PasswordEncoder passwordEncoder;
    private final ServicioJwt servicioJwt;
    private final TokenRevocadoRepositorio tokenRevocadoRepositorio;
    private final PropiedadesApp propiedades;

    public ServicioAutenticacion(CreadorCliente creadorCliente,
                                 CreadorProfesional creadorProfesional,
                                 RepositorioFactory repositorioFactory,
                                 CategoriaOficioRepositorio categoriaOficioRepositorio,
                                 IAlmacenamientoArchivos almacenamientoArchivos,
                                 ValidadorDatosComunes validador,
                                 PasswordEncoder passwordEncoder,
                                 ServicioJwt servicioJwt,
                                 TokenRevocadoRepositorio tokenRevocadoRepositorio,
                                 PropiedadesApp propiedades) {
        this.creadorCliente = creadorCliente;
        this.creadorProfesional = creadorProfesional;
        this.usuarioRepositorio = repositorioFactory.crearUsuarioRepositorio();
        this.categoriaOficioRepositorio = categoriaOficioRepositorio;
        this.almacenamientoArchivos = almacenamientoArchivos;
        this.validador = validador;
        this.passwordEncoder = passwordEncoder;
        this.servicioJwt = servicioJwt;
        this.tokenRevocadoRepositorio = tokenRevocadoRepositorio;
        this.propiedades = propiedades;
    }

    @Transactional
    public RespuestaAuthDTO registrarCliente(SolicitudRegistroClienteDTO solicitud) {
        DatosRegistroUsuario datos = new DatosRegistroUsuario(
                solicitud.nombre(),
                solicitud.correo(),
                solicitud.celular(),
                solicitud.contrasena(),
                solicitud.ubicacion(),
                solicitud.latitud(),
                solicitud.longitud(),
                null,
                List.of(),
                List.of()
        );
        Usuario usuario = creadorCliente.registrar(datos);
        String token = servicioJwt.generarToken(usuario);
        return new RespuestaAuthDTO(token, construirResumen(usuario), null);
    }

    @Transactional
    public RespuestaAuthDTO registrarProfesional(String nombre,
                                                 String correo,
                                                 String celular,
                                                 String contrasena,
                                                 String ubicacion,
                                                 Double latitud,
                                                 Double longitud,
                                                 List<Long> categoriasIds,
                                                 boolean aceptaTerminos,
                                                 MultipartFile documentoIdentidad,
                                                 List<MultipartFile> certificaciones) {
        if (!aceptaTerminos) {
            throw new ExcepcionNegocio("Debes aceptar los términos y condiciones.");
        }
        if (nombre == null || nombre.isBlank() || ubicacion == null || ubicacion.isBlank()) {
            throw new ExcepcionNegocio("El nombre y la ubicación son obligatorios.");
        }
        String correoNorm = validador.normalizarYValidarCorreo(correo);
        if (usuarioRepositorio.existePorCorreo(correoNorm)) {
            throw new ExcepcionConflicto("El correo electrónico ya está en uso.");
        }
        validador.validarCelularColombiano(celular);
        validador.validarContrasena(contrasena);
        validador.validarCoordenadas(latitud, longitud);

        if (categoriasIds == null || categoriasIds.isEmpty()) {
            throw new ExcepcionNegocio("Debes seleccionar al menos una categoría de oficio.");
        }
        Set<Long> idsUnicos = new LinkedHashSet<>(categoriasIds);
        List<CategoriaOficio> categorias = categoriaOficioRepositorio.findAllById(idsUnicos);
        if (categorias.size() != idsUnicos.size()) {
            throw new ExcepcionNegocio("Una o más categorías seleccionadas no existen.");
        }

        validador.validarArchivoDocumento(documentoIdentidad);
        if (certificaciones != null) {
            for (MultipartFile cert : certificaciones) {
                if (cert != null && !cert.isEmpty()) {
                    validador.validarArchivoDocumento(cert);
                }
            }
        }

        String rutaDoc = almacenamientoArchivos.guardarArchivoPrivado(documentoIdentidad, "identidades");
        List<String> rutasCert = new ArrayList<>();
        if (certificaciones != null) {
            for (MultipartFile cert : certificaciones) {
                if (cert != null && !cert.isEmpty()) {
                    rutasCert.add(almacenamientoArchivos.guardarArchivoPrivado(cert, "certificaciones"));
                }
            }
        }

        DatosRegistroUsuario datos = new DatosRegistroUsuario(
                nombre,
                correoNorm,
                celular,
                contrasena,
                ubicacion,
                latitud,
                longitud,
                rutaDoc,
                rutasCert,
                categorias
        );
        Usuario usuario = creadorProfesional.registrar(datos);
        String token = servicioJwt.generarToken(usuario);
        String aviso = "Tu cuenta está en estado Pendiente de verificación. No podrás recibir solicitudes hasta ser aprobada.";
        return new RespuestaAuthDTO(token, construirResumen(usuario), aviso);
    }

    @Transactional(noRollbackFor = {
            ExcepcionNoAutorizado.class,
            ExcepcionCuentaBloqueada.class,
            ExcepcionCuentaRestringida.class
    })
    public RespuestaAuthDTO iniciarSesion(SolicitudLoginDTO solicitud) {
        String correoNorm = solicitud.correo().trim().toLowerCase(Locale.ROOT);
        Usuario usuario = usuarioRepositorio.buscarPorCorreo(correoNorm)
                .orElseThrow(() -> new ExcepcionNoAutorizado(ERROR_CREDENCIALES));

        OffsetDateTime ahora = OffsetDateTime.now();
        if (usuario.levantarSuspensionSiExpiro(ahora)) {
            usuarioRepositorio.guardar(usuario);
        }

        if (usuario.getEstadoCuenta() == EstadoCuenta.SUSPENDIDA) {
            String fechaFin = usuario.getSuspendidoHasta() != null
                    ? usuario.getSuspendidoHasta().format(FORMATO_FECHA_SUSPENSION)
                    : "indefinida";
            String motivo = usuario.getMotivoSuspension() != null
                    ? " Motivo: " + usuario.getMotivoSuspension()
                    : "";
            throw new ExcepcionCuentaRestringida(
                    "Tu cuenta está suspendida temporalmente hasta el " + fechaFin + "." + motivo);
        }

        if (usuario.getEstadoCuenta() == EstadoCuenta.DESACTIVADA) {
            String motivo = usuario.getMotivoSuspension() != null
                    ? " Motivo: " + usuario.getMotivoSuspension()
                    : "";
            throw new ExcepcionCuentaRestringida(
                    "Tu cuenta fue desactivada permanentemente." + motivo);
        }

        if (!usuario.estaActiva()) {
            throw new ExcepcionNoAutorizado("Tu cuenta no se encuentra activa en la plataforma.");
        }

        if (usuario.estaBloqueado(ahora)) {
            throw new ExcepcionCuentaBloqueada("Cuenta bloqueada temporalmente por superar el límite de intentos fallidos.");
        }

        if (!passwordEncoder.matches(solicitud.contrasena(), usuario.getContrasena())) {
            int maxIntentos = propiedades.getAutenticacion().getMaxIntentosFallidos();
            int minutosBloqueo = propiedades.getAutenticacion().getMinutosBloqueo();
            boolean quedoBloqueado = usuario.registrarIntentoFallido(maxIntentos, minutosBloqueo, ahora);
            usuarioRepositorio.guardar(usuario);
            if (quedoBloqueado) {
                throw new ExcepcionCuentaBloqueada("Cuenta bloqueada temporalmente por " + minutosBloqueo + " minutos tras múltiples intentos fallidos.");
            }
            throw new ExcepcionNoAutorizado(ERROR_CREDENCIALES);
        }

        if (usuario.getIntentosFallidos() > 0 || usuario.getBloqueadoHasta() != null) {
            usuario.reiniciarIntentosFallidos();
            usuarioRepositorio.guardar(usuario);
        }

        String token = servicioJwt.generarToken(usuario);
        String aviso = construirAvisoEstado(usuario);
        return new RespuestaAuthDTO(token, construirResumen(usuario), aviso);
    }

    @Transactional
    public void cerrarSesion(String cabeceraAutorizacion) {
        if (cabeceraAutorizacion == null || !cabeceraAutorizacion.startsWith("Bearer ")) {
            return;
        }
        String token = cabeceraAutorizacion.substring(7).trim();
        try {
            Claims claims = servicioJwt.extraerClaims(token);
            String jti = servicioJwt.extraerJti(claims);
            if (jti != null && !tokenRevocadoRepositorio.existsByJti(jti)) {
                tokenRevocadoRepositorio.save(new TokenRevocado(jti, servicioJwt.extraerExpiracion(claims)));
            }
        } catch (JwtException | IllegalArgumentException ignored) {
            // Token invalido o expirado no requiere persistencia adicional
        }
    }

    @Transactional(readOnly = true)
    public ResumenUsuarioDTO obtenerMiPerfil(Long idUsuario) {
        Usuario usuario = usuarioRepositorio.buscarPorId(idUsuario)
                .orElseThrow(() -> new ExcepcionRecursoNoEncontrado("Usuario no encontrado."));
        return construirResumen(usuario);
    }

    public ResumenUsuarioDTO construirResumen(Usuario usuario) {
        if (usuario instanceof Profesional p) {
            return new ResumenUsuarioDTO(
                    p.getIdUsuario(),
                    p.getNombre(),
                    p.getCorreo(),
                    p.getCelular(),
                    p.getRol(),
                    p.getEstadoCuenta(),
                    p.getEstadoVerificacion(),
                    p.getMotivoRechazo(),
                    p.isDisponible(),
                    p.getUbicacion(),
                    p.getLatitud(),
                    p.getLongitud()
            );
        }
        if (usuario instanceof Cliente c) {
            return new ResumenUsuarioDTO(
                    c.getIdUsuario(),
                    c.getNombre(),
                    c.getCorreo(),
                    c.getCelular(),
                    c.getRol(),
                    c.getEstadoCuenta(),
                    null,
                    null,
                    null,
                    c.getUbicacion(),
                    c.getLatitud(),
                    c.getLongitud()
            );
        }
        return new ResumenUsuarioDTO(
                usuario.getIdUsuario(),
                usuario.getNombre(),
                usuario.getCorreo(),
                usuario.getCelular(),
                usuario.getRol(),
                usuario.getEstadoCuenta(),
                null,
                null,
                null,
                null,
                null,
                null
        );
    }

    private String construirAvisoEstado(Usuario usuario) {
        if (usuario instanceof Profesional profesional) {
            if (profesional.getEstadoVerificacion() == EstadoVerificacion.PENDIENTE_VERIFICACION) {
                return "Tu cuenta está en verificación. Aún no puedes recibir solicitudes hasta ser aprobada.";
            }
            if (profesional.getEstadoVerificacion() == EstadoVerificacion.RECHAZADO) {
                return "Tu verificación fue rechazada (" + profesional.getMotivoRechazo() + "). Vuelve a cargar tu documento.";
            }
        }
        return null;
    }
}
