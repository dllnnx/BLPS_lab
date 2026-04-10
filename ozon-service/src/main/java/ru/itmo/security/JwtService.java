package ru.itmo.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;

@Component
public class JwtService {

    private final SecretKey key;
    private final long expirationMs;

    public JwtService(
            @Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.expiration-ms:86400000}") long expirationMs
    ) {
        byte[] bytes = secret.getBytes(StandardCharsets.UTF_8);
        if (bytes.length < 32) {
            throw new IllegalStateException("app.jwt.secret must be at least 32 bytes (256 bits) for HS256");
        }
        this.key = Keys.hmacShaKeyFor(bytes);
        this.expirationMs = expirationMs;
    }

    public String createToken(String username, String boundIp, List<String> privileges, Long pickupPointId) {
        Date now = new Date();
        Date exp = new Date(now.getTime() + expirationMs);
        var builder = Jwts.builder()
                .subject(username)
                .claim("ip", boundIp)
                .claim("priv", privileges)
                .issuedAt(now)
                .expiration(exp);
        if (pickupPointId != null) {
            builder.claim("pp", pickupPointId);
        }
        return builder.signWith(key).compact();
    }

    @SuppressWarnings("unchecked")
    public JwtPayload parseAndValidateIp(String token, String currentClientIp) {
        Claims claims = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
        String boundIp = claims.get("ip", String.class);
        if (boundIp == null || !boundIp.equals(currentClientIp)) {
            throw new JwtIpMismatchException(boundIp, currentClientIp);
        }
        String sub = claims.getSubject();
        List<String> priv = claims.get("priv", List.class);
        Long pp = claims.get("pp", Long.class);
        return new JwtPayload(sub, boundIp, priv != null ? priv : List.of(), pp);
    }

    public record JwtPayload(String username, String boundIp, List<String> privileges, Long pickupPointId) {
    }

    public static class JwtIpMismatchException extends RuntimeException {
        public JwtIpMismatchException(String bound, String current) {
            super("Token was issued for IP " + bound + " but request is from " + current);
        }
    }
}
