package co.edu.udea.alamano.autenticacion_seguridad.seguridad;

import co.edu.udea.alamano.autenticacion_seguridad.dominio.RolUsuario;
import co.edu.udea.alamano.autenticacion_seguridad.dominio.Usuario;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;

public class DetallesUsuario implements UserDetails {

    private final Long idUsuario;
    private final String correo;
    private final String contrasena;
    private final RolUsuario rol;
    private final boolean activa;
    private final boolean noBloqueada;

    public DetallesUsuario(Usuario usuario) {
        this.idUsuario = usuario.getIdUsuario();
        this.correo = usuario.getCorreo();
        this.contrasena = usuario.getContrasena();
        this.rol = usuario.getRol();
        this.activa = usuario.estaActiva() && !usuario.estaSuspendidaVigente(OffsetDateTime.now());
        this.noBloqueada = !usuario.estaBloqueado(OffsetDateTime.now());
    }

    public Long getIdUsuario() {
        return idUsuario;
    }

    public RolUsuario getRol() {
        return rol;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + rol.name()));
    }

    @Override
    public String getPassword() {
        return contrasena;
    }

    @Override
    public String getUsername() {
        return correo;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return noBloqueada;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return activa;
    }
}
