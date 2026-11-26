package co.edu.udea.alamano.autenticacion_seguridad.seguridad;

import co.edu.udea.alamano.autenticacion_seguridad.dominio.Usuario;
import co.edu.udea.alamano.comun.configuracion.PropiedadesApp;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.UUID;

@Service
public class ServicioJwt {

    private final PropiedadesApp propiedades;
    private final SecretKey llaveFirma;

    public ServicioJwt(PropiedadesApp propiedades) {
        this.propiedades = propiedades;
        byte[] bytesSecreto = propiedades.getJwt().getSecret().getBytes(StandardCharsets.UTF_8);
        this.llaveFirma = Keys.hmacShaKeyFor(bytesSecreto);
    }

    public String generarToken(Usuario usuario) {
        Instant ahora = Instant.now();
        Instant expiracion = ahora.plusMillis(propiedades.getJwt().getExpiracionMs());
        return Jwts.builder()
                .id(UUID.randomUUID().toString())
                .subject(usuario.getCorreo())
                .claim("uid", usuario.getIdUsuario())
                .claim("rol", usuario.getRol().name())
                .issuedAt(Date.from(ahora))
                .expiration(Date.from(expiracion))
                .signWith(llaveFirma)
                .compact();
    }

    public Claims extraerClaims(String token) {
        return Jwts.parser()
                .verifyWith(llaveFirma)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public String extraerCorreo(Claims claims) {
        return claims.getSubject();
    }

    public String extraerJti(Claims claims) {
        return claims.getId();
    }

    public OffsetDateTime extraerExpiracion(Claims claims) {
        return claims.getExpiration().toInstant().atOffset(ZoneOffset.UTC);
    }
}
