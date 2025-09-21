package lk.ijse.edu.golankacourier.util;

/**
 * --------------------------------------------
 *
 * @Author Dimantha Kaveen
 * @GitHub: https://github.com/KaveenDK
 * --------------------------------------------
 * @Created 9/21/2025
 * @Project GoLankaCourier
 * --------------------------------------------
 **/

import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Robust JWT helper using jjwt (HS256).
 *
 * Sources for secret (priority):
 * 1. app.jwt.secret
 * 2. jwt.secret
 * 3. env var JWT_SECRET
 *
 * If provided secret is base64 it will be decoded. If the resulting key is smaller than required
 * for HS256, we derive a 32-byte key using SHA-256(secret).
 *
 * Note: For production prefer RS256 with a key pair.
 */
@Component
public class JwtUtil {

    private static final Logger log = LoggerFactory.getLogger(JwtUtil.class);

    @Value("${app.jwt.secret:}")
    private String appJwtSecret;

    @Value("${jwt.secret:}")
    private String jwtSecret;

    @Value("${jwt.access-token-exp-ms:${app.jwt.access-token-exp-ms:900000}}")
    private long accessTokenExpMs;

    private SecretKey key;

    private static final int MINIMUM_HS256_KEY_BYTES = 32; // 256 bits

    @PostConstruct
    public void init() {
        String secretCandidate = firstNonBlank(appJwtSecret, jwtSecret, System.getenv("JWT_SECRET"));

        if (secretCandidate == null || secretCandidate.isBlank()) {
            String msg = "JWT secret not configured. Set app.jwt.secret or jwt.secret in application.properties or set environment variable JWT_SECRET.";
            log.error(msg);
            throw new IllegalStateException(msg);
        }

        // Try to interpret as Base64 first, then fallback to raw bytes, then to SHA-256-derived key if too short.
        SecretKey builtKey = null;
        try {
            byte[] decoded = Decoders.BASE64.decode(secretCandidate);
            if (decoded != null && decoded.length >= MINIMUM_HS256_KEY_BYTES) {
                builtKey = Keys.hmacShaKeyFor(decoded);
                log.debug("Using Base64-decoded secret for JWT HS256 ({} bytes).", decoded.length);
            } else {
                log.warn("Base64 secret decoded but length {} < {} bytes; will derive key with SHA-256.", decoded != null ? decoded.length : 0, MINIMUM_HS256_KEY_BYTES);
            }
        } catch (IllegalArgumentException ex) {
            // not valid base64, we'll handle below
            log.debug("JWT secret is not valid base64 (or decode failed), will use raw secret bytes or derive via SHA-256.");
        }

        if (builtKey == null) {
            // try raw bytes
            byte[] raw = secretCandidate.getBytes(StandardCharsets.UTF_8);
            if (raw.length >= MINIMUM_HS256_KEY_BYTES) {
                builtKey = Keys.hmacShaKeyFor(raw);
                log.debug("Using raw UTF-8 secret for JWT HS256 ({} bytes).", raw.length);
            } else {
                // derive using SHA-256 to produce a 32-byte key
                try {
                    MessageDigest md = MessageDigest.getInstance("SHA-256");
                    byte[] digest = md.digest(raw);
                    builtKey = Keys.hmacShaKeyFor(digest);
                    log.warn("JWT secret was too short ({} bytes). Derived a 32-byte key using SHA-256(secret).", raw.length);
                } catch (NoSuchAlgorithmException e) {
                    String msg = "SHA-256 algorithm not available to derive JWT key: " + e.getMessage();
                    log.error(msg, e);
                    throw new IllegalStateException(msg, e);
                }
            }
        }

        this.key = builtKey;
        log.info("JWT initialized (access token expiry: {} ms).", accessTokenExpMs);
    }

    private static String firstNonBlank(String... inputs) {
        if (inputs == null) return null;
        for (String s : inputs) {
            if (s != null && !s.isBlank()) return s;
        }
        return null;
    }

    /**
     * Generate a JWT containing username and roles.
     *
     * @param username principal (sub)
     * @param roles list of role names
     * @return signed JWT string
     */
    public String generateToken(String username, List<String> roles) {
        long now = System.currentTimeMillis();
        Date issuedAt = new Date(now);
        Date expiry = new Date(now + accessTokenExpMs);

        JwtBuilder b = Jwts.builder()
                .setSubject(username)
                .setIssuedAt(issuedAt)
                .setExpiration(expiry)
                .claim("roles", roles)
                .signWith(key, SignatureAlgorithm.HS256);

        return b.compact();
    }

    /**
     * Validate token signature & expiration.
     *
     * @param token JWT string
     * @return true if valid (signature ok and not expired)
     */
    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token);
            return true;
        } catch (ExpiredJwtException ex) {
            log.debug("JWT expired: {}", ex.getMessage());
            return false;
        } catch (JwtException | IllegalArgumentException ex) {
            log.debug("JWT invalid: {}", ex.getMessage());
            return false;
        }
    }

    public Claims getAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    public String getUsernameFromToken(String token) {
        return getClaim(token, Claims::getSubject);
    }

    @SuppressWarnings("unchecked")
    public List<String> getRolesFromToken(String token) {
        Claims c = getAllClaims(token);
        final Object roles = c.get("roles");
        if (roles instanceof List) {
            return (List<String>) roles;
        }
        return List.of();
    }

    public <T> T getClaim(String token, Function<Claims, T> resolver) {
        final Claims claims = getAllClaims(token);
        return resolver.apply(claims);
    }

    public Date getExpirationDate(String token) {
        return getClaim(token, Claims::getExpiration);
    }

    /**
     * Build a simple map of token metadata (subject, issuedAt, expiration)
     */
    public Map<String, Object> tokenMetadata(String token) {
        Claims c = getAllClaims(token);
        return Map.of(
                "subject", c.getSubject(),
                "issuedAt", c.getIssuedAt(),
                "expiresAt", c.getExpiration()
        );
    }
}
