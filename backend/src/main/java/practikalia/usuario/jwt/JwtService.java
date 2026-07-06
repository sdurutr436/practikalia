package practikalia.usuario.jwt;

import practikalia.usuario.Usuario;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

@Component
public class JwtService {

    public static final String AUTORIDAD_CAMBIO_PENDIENTE = "CAMBIO_CONTRASENA_PENDIENTE";

    private static final String CLAIM_AUTHORITIES = "authorities";
    private static final Duration EXPIRACION_RESTRINGIDA = Duration.ofMinutes(10);

    private final SecretKey clave;
    private final Duration expiracionNormal;

    public JwtService(
            @Value("${jwt.secret:practikalia-dev-secret-cambiar-en-produccion}") String secreto,
            @Value("${jwt.expiration:604800000}") long expiracionNormalMs) {
        this.clave = Keys.hmacShaKeyFor(sha256(secreto));
        this.expiracionNormal = Duration.ofMillis(expiracionNormalMs);
    }

    public String generarTokenNormal(Usuario usuario) {
        List<String> authorities = new ArrayList<>();
        authorities.add("ROLE_" + usuario.getRol().name());
        if (usuario.isEsAdmin()) {
            authorities.add("ADMIN");
        }
        return construirToken(usuario.getCorreo(), authorities, expiracionNormal);
    }

    public String generarTokenRestringido(Usuario usuario) {
        return construirToken(usuario.getCorreo(), List.of(AUTORIDAD_CAMBIO_PENDIENTE), EXPIRACION_RESTRINGIDA);
    }

    public Optional<Jws<Claims>> parsear(String token) {
        try {
            return Optional.of(Jwts.parser().verifyWith(clave).build().parseSignedClaims(token));
        } catch (JwtException | IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    public String correo(Jws<Claims> jws) {
        return jws.getPayload().getSubject();
    }

    @SuppressWarnings("unchecked")
    public List<String> authorities(Jws<Claims> jws) {
        return (List<String>) jws.getPayload().get(CLAIM_AUTHORITIES, List.class);
    }

    private String construirToken(String correo, List<String> authorities, Duration expiracion) {
        Instant ahora = Instant.now();
        return Jwts.builder()
                .subject(correo)
                .claim(CLAIM_AUTHORITIES, authorities)
                .issuedAt(Date.from(ahora))
                .expiration(Date.from(ahora.plus(expiracion)))
                .signWith(clave)
                .compact();
    }

    private static byte[] sha256(String texto) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(texto.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
