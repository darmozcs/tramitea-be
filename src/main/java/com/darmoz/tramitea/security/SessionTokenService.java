package com.darmoz.tramitea.security;

import com.darmoz.tramitea.config.AuthMode;
import com.darmoz.tramitea.config.SessionTokenProperties;
import com.darmoz.tramitea.entity.CitizenUser;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

/**
 * Emite y verifica los tokens de sesion (JWT HS256). Mismo formato que
 * espera el middleware del front: sub = id, claims firstName/lastName/email,
 * iss=tramitea, aud=tramitea-web. Solo existe en modo local (ver AuthMode).
 */
@Service
@ConditionalOnProperty(name = AuthMode.PROPERTY, havingValue = AuthMode.LOCAL, matchIfMissing = true)
public class SessionTokenService implements TokenVerifier {

    private static final String CLAIM_EMAIL = "email";
    private static final String CLAIM_FIRST_NAME = "firstName";
    private static final String CLAIM_LAST_NAME = "lastName";

    private final SecretKey key;
    private final SessionTokenProperties properties;

    public SessionTokenService(SessionTokenProperties properties) {
        String secret = properties.getSecret();
        if (secret == null || secret.length() < SessionTokenProperties.MIN_SECRET_LENGTH) {
            throw new IllegalStateException("auth.secret (AUTH_SECRET) debe tener al menos "
                    + SessionTokenProperties.MIN_SECRET_LENGTH + " caracteres");
        }
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.properties = properties;
    }

    public String issue(CitizenUser user) {
        Instant issuedAt = Instant.now();
        return Jwts.builder()
                .subject(user.getId().toString())
                .claim(CLAIM_FIRST_NAME, user.getFirstName())
                .claim(CLAIM_LAST_NAME, user.getLastName())
                .claim(CLAIM_EMAIL, user.getEmail())
                .issuer(properties.getIssuer())
                .audience().add(properties.getAudience()).and()
                .issuedAt(Date.from(issuedAt))
                .expiration(Date.from(issuedAt.plusSeconds(properties.getTtlSeconds())))
                .signWith(key, Jwts.SIG.HS256)
                .compact();
    }

    public long getTtlSeconds() {
        return properties.getTtlSeconds();
    }

    /** Valida firma, expiracion, issuer y audience. Lanza {@link JwtException} ante cualquier fallo. */
    @Override
    public AuthenticatedUser verify(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(key)
                .requireIssuer(properties.getIssuer())
                .requireAudience(properties.getAudience())
                .build()
                .parseSignedClaims(token)
                .getPayload();
        return new AuthenticatedUser(UUID.fromString(claims.getSubject()), claims.get(CLAIM_EMAIL, String.class));
    }
}
