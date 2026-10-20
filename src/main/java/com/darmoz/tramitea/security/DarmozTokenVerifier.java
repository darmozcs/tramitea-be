package com.darmoz.tramitea.security;

import com.darmoz.tramitea.config.AuthMode;
import com.darmoz.tramitea.config.DarmozAuthProperties;
import com.darmoz.tramitea.entity.CitizenUser;
import com.darmoz.tramitea.service.CitizenUserService;
import com.darmoz.tramitea.util.PemUtils;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.security.PublicKey;
import java.util.UUID;

/**
 * Modo darmoz: verifica (nunca emite) los access tokens RS256 de darmoz-auth
 * (adaptado de guardias-sa-be) y los traduce al usuario de Tramitea vinculado
 * por tr_users.id_auth_user. El principal lleva el id propio de Tramitea, así
 * que el resto de los servicios no distingue el modo.
 */
@Service
@ConditionalOnProperty(name = AuthMode.PROPERTY, havingValue = AuthMode.DARMOZ)
public class DarmozTokenVerifier implements TokenVerifier {

    private static final String CLAIM_EMAIL = "email";
    private static final String CLAIM_APPLICATION_ID = "applicationId";

    private final PublicKey verificationKey;
    private final DarmozAuthProperties properties;
    private final CitizenUserService citizenUserService;

    public DarmozTokenVerifier(DarmozAuthProperties properties, CitizenUserService citizenUserService) {
        if (isBlank(properties.getPublicKeyPath()) || isBlank(properties.getApplicationId())) {
            throw new IllegalStateException("Con AUTH_MODE=darmoz hay que configurar "
                    + "DARMOZ_AUTH_PUBLIC_KEY_PATH y DARMOZ_AUTH_APPLICATION_ID");
        }
        this.verificationKey = PemUtils.readPublicKey(Path.of(properties.getPublicKeyPath()));
        this.properties = properties;
        this.citizenUserService = citizenUserService;
    }

    @Override
    public AuthenticatedUser verify(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(verificationKey)
                .requireIssuer(properties.getIssuer())
                .build()
                .parseSignedClaims(token)
                .getPayload();

        String applicationId = claims.get(CLAIM_APPLICATION_ID, String.class);
        if (!properties.getApplicationId().equals(applicationId)) {
            throw new JwtException("applicationId inesperado: " + applicationId);
        }

        UUID idAuthUser = UUID.fromString(claims.getSubject());
        String email = claims.get(CLAIM_EMAIL, String.class);

        CitizenUser user;
        try {
            user = citizenUserService.resolveByAuthUser(idAuthUser, email);
        } catch (DataIntegrityViolationException e) {
            // Otro request del mismo usuario nuevo lo creó en paralelo: ya existe.
            user = citizenUserService.resolveByAuthUser(idAuthUser, email);
        }
        return new AuthenticatedUser(user.getId(), user.getEmail());
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
