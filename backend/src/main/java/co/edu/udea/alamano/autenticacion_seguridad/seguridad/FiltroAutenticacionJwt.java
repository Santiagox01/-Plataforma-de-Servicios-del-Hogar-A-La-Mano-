package co.edu.udea.alamano.autenticacion_seguridad.seguridad;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class FiltroAutenticacionJwt extends OncePerRequestFilter {

    private final ServicioJwt servicioJwt;
    private final ServicioDetallesUsuario servicioDetallesUsuario;
    private final TokenRevocadoRepositorio tokenRevocadoRepositorio;

    public FiltroAutenticacionJwt(ServicioJwt servicioJwt,
                                   ServicioDetallesUsuario servicioDetallesUsuario,
                                   TokenRevocadoRepositorio tokenRevocadoRepositorio) {
        this.servicioJwt = servicioJwt;
        this.servicioDetallesUsuario = servicioDetallesUsuario;
        this.tokenRevocadoRepositorio = tokenRevocadoRepositorio;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String cabecera = request.getHeader("Authorization");
        if (cabecera != null && cabecera.startsWith("Bearer ")) {
            String token = cabecera.substring(7).trim();
            try {
                Claims claims = servicioJwt.extraerClaims(token);
                String jti = servicioJwt.extraerJti(claims);
                String correo = servicioJwt.extraerCorreo(claims);

                if (jti != null && !tokenRevocadoRepositorio.existsByJti(jti)
                        && correo != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                    UserDetails detalles = servicioDetallesUsuario.loadUserByUsername(correo);
                    if (detalles.isEnabled() && detalles.isAccountNonLocked()) {
                        UsernamePasswordAuthenticationToken auth =
                                new UsernamePasswordAuthenticationToken(detalles, null, detalles.getAuthorities());
                        auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                        SecurityContextHolder.getContext().setAuthentication(auth);
                    }
                }
            } catch (JwtException | IllegalArgumentException ignored) {
                SecurityContextHolder.clearContext();
            }
        }
        filterChain.doFilter(request, response);
    }
}
