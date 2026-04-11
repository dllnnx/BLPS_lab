package ru.itmo.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Date;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

@Component
public class JwtService {

    private final SecretKey key;
    private final long expirationMs;
    private final JwtBlacklistService jwtBlacklistService;

    public JwtService(
            @Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.expiration-ms:86400000}") long expirationMs,
            JwtBlacklistService jwtBlacklistService
    ) {
        byte[] bytes = secret.getBytes(StandardCharsets.UTF_8);
        if (bytes.length < 32) {
            throw new IllegalStateException("app.jwt.secret must be at least 32 bytes (256 bits) for HS256");
        }
        this.key = Keys.hmacShaKeyFor(bytes);
        this.expirationMs = expirationMs;
        this.jwtBlacklistService = jwtBlacklistService;
    }

    public String createToken(String username, String boundIp, List<String> privileges, Long pickupPointId) {
        Date now = new Date();
        Date exp = new Date(now.getTime() + expirationMs);
        var builder = Jwts.builder()
                .id(UUID.randomUUID().toString())
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

        long expMs = claims.getExpiration() != null
                ? claims.getExpiration().getTime()
                : System.currentTimeMillis() + expirationMs;

        String revokeKey = revocationKey(token, claims);
        if (jwtBlacklistService.isRevoked(revokeKey)) {
            throw new JwtRevokedException();
        }

        String boundIp = claims.get("ip", String.class);
        if (boundIp == null || !normalizeIp(boundIp).equals(normalizeIp(currentClientIp))) {
            jwtBlacklistService.revokeUntil(revokeKey, expMs);
            throw new JwtIpMismatchException(boundIp, currentClientIp);
        }

        String sub = claims.getSubject();
        List<String> priv = claims.get("priv", List.class);
        Long pp = claims.get("pp", Long.class);
        return new JwtPayload(sub, normalizeIp(boundIp), priv != null ? priv : List.of(), pp);
    }

    private static String revocationKey(String rawToken, Claims claims) {
        String jti = claims.getId();
        if (jti != null && !jti.isBlank()) {
            return jti;
        }
        return "t:" + sha256Hex(rawToken);
    }

    private static String normalizeIp(String ip) {
        if (ip == null) {
            return "";
        }
        String t = ip.trim();
        if (t.startsWith("[") && t.contains("]")) {
            int end = t.indexOf(']');
            return t.substring(1, end).trim();
        }
        return t;
    }

    private static String sha256Hex(String s) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(md.digest(s.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    public record JwtPayload(String username, String boundIp, List<String> privileges, Long pickupPointId) {
    }

    public static class JwtRevokedException extends RuntimeException {
        public JwtRevokedException() {
            super("Token was revoked (e.g. used from a different IP)");
        }
    }

    public static class JwtIpMismatchException extends RuntimeException {
        public JwtIpMismatchException(String bound, String current) {
            super("Token was issued for IP " + bound + " but request is from " + current + "; token revoked");
        }
    }
}
