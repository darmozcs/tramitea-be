package com.darmoz.tramitea.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Verificación de los access tokens de darmoz-auth (solo con auth.mode=darmoz).
 * El servicio nunca los emite: solo valida la firma RS256 con la llave pública
 * de darmoz-auth, el issuer y que el token sea de la Application de Tramitea.
 */
@ConfigurationProperties(prefix = "darmoz-auth")
public class DarmozAuthProperties {

    /** PEM X.509 con la llave pública RSA con la que firma darmoz-auth. */
    private String publicKeyPath;

    private String issuer = "darmoz-auth";

    /** UUID de la Application "Tramitea" en darmoz-auth (claim applicationId). */
    private String applicationId;

    public String getPublicKeyPath() { return publicKeyPath; }
    public void setPublicKeyPath(String publicKeyPath) { this.publicKeyPath = publicKeyPath; }
    public String getIssuer() { return issuer; }
    public void setIssuer(String issuer) { this.issuer = issuer; }
    public String getApplicationId() { return applicationId; }
    public void setApplicationId(String applicationId) { this.applicationId = applicationId; }
}
