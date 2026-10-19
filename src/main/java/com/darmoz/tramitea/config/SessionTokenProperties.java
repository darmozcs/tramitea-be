package com.darmoz.tramitea.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Token de sesion HS256. El secreto es el mismo AUTH_SECRET que usa el front
 * (Next.js) para verificar la cookie en su middleware, por eso issuer,
 * audience y claims replican tramitea/src/services/user/sessionToken.ts.
 */
@ConfigurationProperties(prefix = "auth")
public class SessionTokenProperties {

    public static final int MIN_SECRET_LENGTH = 32;

    private String secret;

    private String issuer = "tramitea";

    private String audience = "tramitea-web";

    private long ttlSeconds = 30 * 60;

    public String getSecret() { return secret; }
    public void setSecret(String secret) { this.secret = secret; }
    public String getIssuer() { return issuer; }
    public void setIssuer(String issuer) { this.issuer = issuer; }
    public String getAudience() { return audience; }
    public void setAudience(String audience) { this.audience = audience; }
    public long getTtlSeconds() { return ttlSeconds; }
    public void setTtlSeconds(long ttlSeconds) { this.ttlSeconds = ttlSeconds; }
}
