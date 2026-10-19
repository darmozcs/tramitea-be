package com.darmoz.tramitea.integration;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

/** Tokens armados a mano para probar los rechazos (firma, issuer, expiracion). */
public final class TestJwtFactory {

    private TestJwtFactory() {
    }

    public static String issueToken(UUID userId, String secret, String issuer, Instant expiresAt) {
        return Jwts.builder()
                .subject(userId.toString())
                .claim("firstName", "X")
                .claim("lastName", "Y")
                .claim("email", "x@example.com")
                .issuer(issuer)
                .audience().add("tramitea-web").and()
                .issuedAt(Date.from(expiresAt.minusSeconds(60)))
                .expiration(Date.from(expiresAt))
                .signWith(Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8)), Jwts.SIG.HS256)
                .compact();
    }
}
