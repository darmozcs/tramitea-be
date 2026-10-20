package com.darmoz.tramitea.integration;

import com.darmoz.tramitea.entity.CitizenUser;
import com.darmoz.tramitea.repository.CitizenUserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import io.jsonwebtoken.Jwts;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * AUTH_MODE=darmoz: los tokens los emite darmoz-auth (RS256) y tr_users se
 * relaciona por id_auth_user. Se simula darmoz-auth firmando con un par RSA
 * generado para el test, con el mismo shape de claims que su JwtService.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class DarmozAuthModeIT extends AbstractIntegrationTest {

    static final String APPLICATION_ID = "33333333-3333-4333-8333-333333333333";
    static final String CEDULA_ID = ProcedureControllerIT.CEDULA_ID;

    static final PrivateKey DARMOZ_PRIVATE_KEY;
    static final Path DARMOZ_PUBLIC_KEY_PATH;

    static {
        try {
            KeyPair keyPair = KeyPairGenerator.getInstance("RSA").generateKeyPair();
            DARMOZ_PRIVATE_KEY = keyPair.getPrivate();
            DARMOZ_PUBLIC_KEY_PATH = Files.createTempDirectory("tramitea-be-darmoz-keys").resolve("public_key.pem");
            String base64 = Base64.getEncoder().encodeToString(keyPair.getPublic().getEncoded());
            Files.writeString(DARMOZ_PUBLIC_KEY_PATH,
                    "-----BEGIN PUBLIC KEY-----\n" + base64 + "\n-----END PUBLIC KEY-----\n");
        } catch (NoSuchAlgorithmException | IOException e) {
            throw new IllegalStateException("No se pudieron generar las llaves RSA de test", e);
        }
    }

    @DynamicPropertySource
    static void darmozProperties(DynamicPropertyRegistry registry) {
        registry.add("auth.mode", () -> "darmoz");
        registry.add("darmoz-auth.public-key-path", DARMOZ_PUBLIC_KEY_PATH::toString);
        registry.add("darmoz-auth.application-id", () -> APPLICATION_ID);
    }

    @Autowired
    private CitizenUserRepository citizenUserRepository;

    @Test
    void firstRequestCreatesProfileLinkedByIdAuthUser() throws Exception {
        UUID authUserId = UUID.randomUUID();
        String token = darmozToken(authUserId, "nueva-" + authUserId + "@example.com", APPLICATION_ID, DARMOZ_PRIVATE_KEY);

        JsonNode profile = body(call(HttpMethod.GET, "/user/profile", null, token));

        CitizenUser stored = citizenUserRepository.findByIdAuthUser(authUserId).orElseThrow();
        assertThat(profile.get("id").asText()).isEqualTo(stored.getId().toString());
        assertThat(stored.getId()).isNotEqualTo(authUserId);
        assertThat(stored.getPasswordHash()).isNull();
        assertThat(profile.get("firstName").asText()).isEmpty();

        // Segundo request: misma fila, no se duplica.
        body(call(HttpMethod.GET, "/user/profile", null, token));
        assertThat(citizenUserRepository.findAll().stream()
                .filter(u -> authUserId.equals(u.getIdAuthUser())).count()).isEqualTo(1);
    }

    @Test
    void userDataUsesTramiteaIdNotDarmozId() throws Exception {
        UUID authUserId = UUID.randomUUID();
        String token = darmozToken(authUserId, "tareas-" + authUserId + "@example.com", APPLICATION_ID, DARMOZ_PRIVATE_KEY);

        JsonNode task = body(call(HttpMethod.POST, "/user/saved-tasks", Map.of("procedureId", CEDULA_ID), token));

        UUID tramiteaId = citizenUserRepository.findByIdAuthUser(authUserId).orElseThrow().getId();
        assertThat(task.get("userId").asText()).isEqualTo(tramiteaId.toString());
    }

    @Test
    void profileUpdateKeepsDarmozEmailAndSavesNames() throws Exception {
        UUID authUserId = UUID.randomUUID();
        String email = "perfil-" + authUserId + "@example.com";
        String token = darmozToken(authUserId, email, APPLICATION_ID, DARMOZ_PRIVATE_KEY);

        JsonNode updated = body(call(HttpMethod.PATCH, "/user/profile", Map.of(
                "firstName", "Ana", "lastName", "Paz", "email", "otro@example.com"), token));

        assertThat(updated.get("firstName").asText()).isEqualTo("Ana");
        assertThat(updated.get("email").asText()).isEqualTo(email);
    }

    @Test
    void existingLocalProfileWithSameEmailIsLinked() throws Exception {
        String email = "local-" + UUID.randomUUID() + "@example.com";
        CitizenUser local = citizenUserRepository.save(new CitizenUser(email, "hash", "Luis", "Sosa"));
        UUID authUserId = UUID.randomUUID();

        JsonNode profile = body(call(HttpMethod.GET, "/user/profile", null,
                darmozToken(authUserId, email, APPLICATION_ID, DARMOZ_PRIVATE_KEY)));

        assertThat(profile.get("id").asText()).isEqualTo(local.getId().toString());
        assertThat(profile.get("firstName").asText()).isEqualTo("Luis");
        assertThat(citizenUserRepository.findById(local.getId()).orElseThrow().getIdAuthUser()).isEqualTo(authUserId);
    }

    @Test
    void rejectsTokensFromOtherApplicationsOrSigners() throws Exception {
        UUID someone = UUID.randomUUID();
        String otherApp = darmozToken(someone, "x@example.com", UUID.randomUUID().toString(), DARMOZ_PRIVATE_KEY);
        PrivateKey foreignKey = KeyPairGenerator.getInstance("RSA").generateKeyPair().getPrivate();
        String forged = darmozToken(someone, "x@example.com", APPLICATION_ID, foreignKey);

        for (String token : new String[]{otherApp, forged}) {
            assertThat(call(HttpMethod.GET, "/user/profile", null, token).getStatusCode())
                    .isEqualTo(HttpStatus.UNAUTHORIZED);
        }
        assertThat(citizenUserRepository.findByIdAuthUser(someone)).isEmpty();
    }

    @Test
    void localAuthEndpointsDoNotExist() {
        assertThat(call(HttpMethod.POST, "/auth/sign-in", Map.of("email", "a@b.uy", "password", "x"), null)
                .getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(call(HttpMethod.POST, "/auth/register", Map.of("email", "a@b.uy"), null)
                .getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    /** Mismo shape de claims que el access token de darmoz-auth (ver su API_GUIDE §4). */
    private static String darmozToken(UUID userId, String email, String applicationId, PrivateKey signingKey) {
        Instant now = Instant.now();
        return Jwts.builder()
                .id(UUID.randomUUID().toString())
                .subject(userId.toString())
                .claim("email", email)
                .claim("roles", List.of("USER"))
                .claim("permissions", List.of())
                .claim("applicationId", applicationId)
                .claim("typ", "access")
                .issuer("darmoz-auth")
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(720)))
                .signWith(signingKey, Jwts.SIG.RS256)
                .compact();
    }
}
