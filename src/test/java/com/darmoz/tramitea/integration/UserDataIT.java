package com.darmoz.tramitea.integration;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/** Tareas guardadas y perfil ciudadano (rutas /user/**). */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class UserDataIT extends AbstractIntegrationTest {

    static final String CEDULA_ID = ProcedureControllerIT.CEDULA_ID;
    static final String PASAPORTE_ID = "cccccccc-cccc-4ccc-8ccc-000000000002";

    private String token;
    private String otherToken;

    @BeforeEach
    void setUp() throws Exception {
        token = registerUser("Lucia");
        otherToken = registerUser("Otro");
    }

    @Test
    void savedTaskLifecycle() throws Exception {
        JsonNode task = body(call(HttpMethod.POST, "/user/saved-tasks", Map.of("procedureId", CEDULA_ID), token));
        assertThat(task.get("status").asText()).isEqualTo("PENDING");
        String taskId = task.get("id").asText();

        // Idempotente.
        JsonNode again = body(call(HttpMethod.POST, "/user/saved-tasks", Map.of("procedureId", CEDULA_ID), token));
        assertThat(again.get("id").asText()).isEqualTo(taskId);

        assertThat(body(call(HttpMethod.GET, "/user/saved-tasks/" + CEDULA_ID + "/is-saved", null, token))
                .asBoolean()).isTrue();

        JsonNode completed = body(call(HttpMethod.PATCH, "/user/saved-tasks/" + taskId,
                Map.of("status", "COMPLETED"), token));
        assertThat(completed.get("completedAt")).isNotNull();

        JsonNode reopened = body(call(HttpMethod.PATCH, "/user/saved-tasks/" + taskId,
                Map.of("status", "IN_PROGRESS"), token));
        assertThat(reopened.has("completedAt")).isFalse();

        JsonNode noted = body(call(HttpMethod.PATCH, "/user/saved-tasks/" + taskId + "/notes",
                Map.of("notes", "  Turno el 28/10  "), token));
        assertThat(noted.get("notes").asText()).isEqualTo("Turno el 28/10");

        JsonNode cleared = body(call(HttpMethod.PATCH, "/user/saved-tasks/" + taskId + "/notes",
                Map.of("notes", "   "), token));
        assertThat(cleared.has("notes")).isFalse();

        assertThat(call(HttpMethod.DELETE, "/user/saved-tasks/" + taskId, null, token).getStatusCode())
                .isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(body(call(HttpMethod.GET, "/user/saved-tasks", null, token))).isEmpty();
    }

    @Test
    void savedTasksAreIsolatedPerUser() throws Exception {
        String taskId = body(call(HttpMethod.POST, "/user/saved-tasks", Map.of("procedureId", PASAPORTE_ID), token))
                .get("id").asText();

        assertThat(body(call(HttpMethod.GET, "/user/saved-tasks", null, otherToken))).isEmpty();
        assertThat(call(HttpMethod.PATCH, "/user/saved-tasks/" + taskId, Map.of("status", "COMPLETED"), otherToken)
                .getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);

        call(HttpMethod.DELETE, "/user/saved-tasks/" + taskId, null, otherToken);
        assertThat(body(call(HttpMethod.GET, "/user/saved-tasks", null, token))).hasSize(1);
    }

    @Test
    void savingUnknownProcedureIs404() {
        assertThat(call(HttpMethod.POST, "/user/saved-tasks", Map.of("procedureId", UUID.randomUUID().toString()),
                token).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void profileIsReadAndUpdated() throws Exception {
        JsonNode profile = body(call(HttpMethod.GET, "/user/profile", null, token));
        assertThat(profile.get("firstName").asText()).isEqualTo("Lucia");
        assertThat(profile.get("nationalIdVerified").asBoolean()).isFalse();
        assertThat(profile.get("notificationPreferences").get("documentExpiry").asBoolean()).isTrue();
        assertThat(profile.get("memberSince")).isNotNull();

        String newEmail = "lucia.nueva-" + UUID.randomUUID() + "@example.com";
        JsonNode updated = body(call(HttpMethod.PATCH, "/user/profile", Map.of(
                "firstName", " Lucía ", "lastName", "Rodríguez", "email", newEmail.toUpperCase(),
                "phone", "  ", "department", "MO", "locality", "Pocitos"), token));
        assertThat(updated.get("firstName").asText()).isEqualTo("Lucía");
        assertThat(updated.get("email").asText()).isEqualTo(newEmail);
        assertThat(updated.get("department").asText()).isEqualTo("MO");
        assertThat(updated.has("phone")).isFalse();

        JsonNode preferences = body(call(HttpMethod.PATCH, "/user/profile/notification-preferences", Map.of(
                "documentExpiry", false, "appointmentReminders", true, "newsAndUpdates", true), token));
        assertThat(preferences.get("documentExpiry").asBoolean()).isFalse();
        assertThat(preferences.get("newsAndUpdates").asBoolean()).isTrue();
    }

    @Test
    void profileEmailMustStayUnique() throws Exception {
        String takenEmail = body(call(HttpMethod.GET, "/user/profile", null, otherToken)).get("email").asText();

        ResponseEntity<String> response = call(HttpMethod.PATCH, "/user/profile",
                Map.of("firstName", "Lucia", "lastName", "Prueba", "email", takenEmail), token);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(errorCode(response)).isEqualTo("email-already-registered");
    }

    @Test
    void invalidProfileIsRejectedWithFrontendCode() throws Exception {
        ResponseEntity<String> response = call(HttpMethod.PATCH, "/user/profile",
                Map.of("firstName", "", "lastName", "X", "email", "sin-arroba"), token);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(errorCode(response)).isEqualTo("invalid-profile-payload");
    }

    @Test
    void deleteAccountRemovesDataAndAnonymizesComments() throws Exception {
        String email = body(call(HttpMethod.GET, "/user/profile", null, token)).get("email").asText();
        String commentId = body(call(HttpMethod.POST, "/procedures/" + CEDULA_ID + "/comments",
                Map.of("content", "Comentario a anonimizar"), token)).get("id").asText();
        call(HttpMethod.POST, "/user/saved-tasks", Map.of("procedureId", CEDULA_ID), token);

        assertThat(call(HttpMethod.DELETE, "/user/profile", null, token).getStatusCode())
                .isEqualTo(HttpStatus.NO_CONTENT);

        JsonNode comments = body(call(HttpMethod.GET, "/procedures/" + CEDULA_ID + "/comments", null, null));
        JsonNode anonymized = null;
        for (JsonNode comment : comments) {
            if (comment.get("id").asText().equals(commentId)) {
                anonymized = comment;
            }
        }
        assertThat(anonymized).isNotNull();
        assertThat(anonymized.get("author").get("displayName").asText()).isEqualTo("Usuario eliminado");

        // El token sigue firmado pero la cuenta ya no existe: 401, y el login tampoco funciona.
        assertThat(call(HttpMethod.GET, "/user/profile", null, token).getStatusCode())
                .isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(call(HttpMethod.POST, "/auth/sign-in", Map.of("email", email, "password", "secreta-123"), null)
                .getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }
}
