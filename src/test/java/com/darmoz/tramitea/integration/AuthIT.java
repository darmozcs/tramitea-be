package com.darmoz.tramitea.integration;

import com.fasterxml.jackson.databind.JsonNode;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AuthIT extends AbstractIntegrationTest {

    @Test
    void registerIssuesTokenInFrontendFormat() throws Exception {
        String email = "Lucia-" + UUID.randomUUID() + "@Example.com";
        JsonNode auth = body(call(HttpMethod.POST, "/auth/register", Map.of(
                "firstName", " Lucía ", "lastName", "Rodríguez", "email", email, "password", "secreta-123"), null));

        assertThat(auth.get("user").get("firstName").asText()).isEqualTo("Lucía");
        assertThat(auth.get("user").get("email").asText()).isEqualTo(email.toLowerCase());
        assertThat(auth.get("expiresIn").asLong()).isEqualTo(1800);

        // Mismos claims que verifica tramitea/src/services/user/sessionToken.ts.
        Claims claims = Jwts.parser()
                .verifyWith(Keys.hmacShaKeyFor(TEST_SECRET.getBytes(StandardCharsets.UTF_8)))
                .requireIssuer("tramitea")
                .requireAudience("tramitea-web")
                .build()
                .parseSignedClaims(auth.get("token").asText())
                .getPayload();
        assertThat(claims.getSubject()).isEqualTo(auth.get("user").get("id").asText());
        assertThat(claims.get("firstName", String.class)).isEqualTo("Lucía");
        assertThat(claims.get("lastName", String.class)).isEqualTo("Rodríguez");
        assertThat(claims.get("email", String.class)).isEqualTo(email.toLowerCase());
    }

    @Test
    void duplicateEmailIsConflictRegardlessOfCase() throws Exception {
        String email = "dup-" + UUID.randomUUID() + "@example.com";
        Map<String, String> payload = Map.of("firstName", "A", "lastName", "B", "email", email,
                "password", "secreta-123");
        body(call(HttpMethod.POST, "/auth/register", payload, null));

        ResponseEntity<String> again = call(HttpMethod.POST, "/auth/register",
                Map.of("firstName", "A", "lastName", "B", "email", email.toUpperCase(), "password", "secreta-123"),
                null);

        assertThat(again.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(errorCode(again)).isEqualTo("email-already-registered");
    }

    @Test
    void invalidRegistrationIs400() throws Exception {
        ResponseEntity<String> shortPassword = call(HttpMethod.POST, "/auth/register", Map.of(
                "firstName", "A", "lastName", "B", "email", "a@example.com", "password", "corta"), null);

        assertThat(shortPassword.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(errorCode(shortPassword)).isEqualTo("invalid-payload");
    }

    @Test
    void signInChecksPassword() throws Exception {
        String email = "login-" + UUID.randomUUID() + "@example.com";
        body(call(HttpMethod.POST, "/auth/register", Map.of(
                "firstName", "Ana", "lastName", "Paz", "email", email, "password", "secreta-123"), null));

        JsonNode ok = body(call(HttpMethod.POST, "/auth/sign-in",
                Map.of("email", "  " + email.toUpperCase() + " ", "password", "secreta-123"), null));
        assertThat(ok.get("user").get("firstName").asText()).isEqualTo("Ana");

        ResponseEntity<String> wrong = call(HttpMethod.POST, "/auth/sign-in",
                Map.of("email", email, "password", "otra-cosa"), null);
        assertThat(wrong.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(errorCode(wrong)).isEqualTo("invalid-credentials");

        ResponseEntity<String> unknown = call(HttpMethod.POST, "/auth/sign-in",
                Map.of("email", "nadie-" + UUID.randomUUID() + "@example.com", "password", "secreta-123"), null);
        assertThat(unknown.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(errorCode(unknown)).isEqualTo("invalid-credentials");
    }

    @Test
    void refreshReflectsProfileChanges() throws Exception {
        String token = registerUser("Ana");
        JsonNode profile = body(call(HttpMethod.GET, "/user/profile", null, token));

        body(call(HttpMethod.PATCH, "/user/profile", Map.of("firstName", "Anabel", "lastName", "Paz",
                "email", profile.get("email").asText()), token));
        JsonNode refreshed = body(call(HttpMethod.POST, "/auth/refresh", null, token));

        assertThat(refreshed.get("user").get("firstName").asText()).isEqualTo("Anabel");
        assertThat(refreshed.get("token").asText()).isNotBlank();
    }

    @Test
    void rejectsForgedExpiredAndForeignTokens() {
        UUID someone = UUID.randomUUID();
        Instant inAnHour = Instant.now().plusSeconds(3600);

        String forged = TestJwtFactory.issueToken(someone, "otro-secreto-distinto-de-al-menos-32-chars", "tramitea",
                inAnHour);
        String wrongIssuer = TestJwtFactory.issueToken(someone, TEST_SECRET, "darmoz-auth", inAnHour);
        String expired = TestJwtFactory.issueToken(someone, TEST_SECRET, "tramitea", Instant.now().minusSeconds(5));
        // Firma valida pero usuario inexistente (p. ej. cuenta borrada): 401, no se recrea.
        String deletedUser = TestJwtFactory.issueToken(someone, TEST_SECRET, "tramitea", inAnHour);

        for (String token : new String[]{forged, wrongIssuer, expired, deletedUser}) {
            assertThat(call(HttpMethod.GET, "/user/profile", null, token).getStatusCode())
                    .isEqualTo(HttpStatus.UNAUTHORIZED);
        }
        assertThat(call(HttpMethod.GET, "/user/profile", null, null).getStatusCode())
                .isEqualTo(HttpStatus.UNAUTHORIZED);
    }
}
