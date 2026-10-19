package com.darmoz.tramitea.integration;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class CommunityIT extends AbstractIntegrationTest {

    static final String CEDULA_ID = ProcedureControllerIT.CEDULA_ID;
    static final String INSTITUTION_ID = "bbbbbbbb-bbbb-4bbb-8bbb-000000000001";


    private String token;

    @BeforeEach
    void setUp() throws Exception {
        token = registerUser("Vecina");
    }

    @Test
    void seededCommentsAreReturnedOldestFirst() throws Exception {
        ResponseEntity<String> response = restTemplate.getForEntity(
                "/procedures/" + CEDULA_ID + "/comments", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode comments = objectMapper.readTree(response.getBody());
        assertThat(comments).isNotEmpty();
        assertThat(comments.get(0).get("author").get("displayName").asText()).isEqualTo("Martín Sosa");
        assertThat(comments.get(0).get("author").get("isVerifiedCitizen").asBoolean()).isTrue();
    }

    @Test
    void postingRequiresAuthentication() {
        ResponseEntity<String> response = restTemplate.postForEntity(
                "/procedures/" + CEDULA_ID + "/comments", json(Map.of("content", "hola"), null), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void postCommentAndReply() throws Exception {
        ResponseEntity<String> created = restTemplate.postForEntity("/procedures/" + CEDULA_ID + "/comments",
                json(Map.of("content", "  Muy util la guia  "), token), String.class);

        assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        JsonNode comment = objectMapper.readTree(created.getBody());
        assertThat(comment.get("content").asText()).isEqualTo("Muy util la guia");
        // Nombre y apellido del registro.
        assertThat(comment.get("author").get("displayName").asText()).isEqualTo("Vecina Prueba");
        assertThat(comment.has("parentId")).isFalse();

        ResponseEntity<String> reply = restTemplate.postForEntity("/procedures/" + CEDULA_ID + "/comments",
                json(Map.of("content", "Coincido", "parentId", comment.get("id").asText()), token), String.class);
        assertThat(reply.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(objectMapper.readTree(reply.getBody()).get("parentId").asText())
                .isEqualTo(comment.get("id").asText());
    }

    @Test
    void replyToCommentOfAnotherProcedureIsRejected() throws Exception {
        String otherProcedure = "cccccccc-cccc-4ccc-8ccc-000000000007";
        JsonNode comment = objectMapper.readTree(restTemplate.postForEntity(
                "/procedures/" + otherProcedure + "/comments", json(Map.of("content", "otro"), token),
                String.class).getBody());

        ResponseEntity<String> response = restTemplate.postForEntity("/procedures/" + CEDULA_ID + "/comments",
                json(Map.of("content", "x", "parentId", comment.get("id").asText()), token), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(objectMapper.readTree(response.getBody()).get("error").asText())
                .isEqualTo("invalid-comment-payload");
    }

    @Test
    void blankCommentIs400() {
        ResponseEntity<String> response = restTemplate.postForEntity("/procedures/" + CEDULA_ID + "/comments",
                json(Map.of("content", "   "), token), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void votingIsOnePerUserAndCanChange() {
        long before = restTemplate.getForObject("/procedures/" + CEDULA_ID + "/usefulness", Long.class);

        Long afterUseful = restTemplate.postForObject("/procedures/" + CEDULA_ID + "/vote",
                json(Map.of("value", "USEFUL"), token), Long.class);
        Long afterRepeat = restTemplate.postForObject("/procedures/" + CEDULA_ID + "/vote",
                json(Map.of("value", "USEFUL"), token), Long.class);
        Long afterChange = restTemplate.postForObject("/procedures/" + CEDULA_ID + "/vote",
                json(Map.of("value", "NOT_USEFUL"), token), Long.class);

        assertThat(afterUseful).isEqualTo(before + 1);
        assertThat(afterRepeat).isEqualTo(before + 1);
        assertThat(afterChange).isEqualTo(before);
    }

    @Test
    void anonymousSuggestionIsAccepted() throws Exception {
        ResponseEntity<String> response = restTemplate.postForEntity("/procedures/suggestions",
                json(Map.of("title", "Permiso de pesca", "institutionId", INSTITUTION_ID,
                        "details", "Falta este tramite"), null), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        JsonNode body = objectMapper.readTree(response.getBody());
        assertThat(body.get("title").asText()).isEqualTo("Permiso de pesca");
        assertThat(body.get("institutionId").asText()).isEqualTo(INSTITUTION_ID);
    }

    @Test
    void suggestionWithUnknownInstitutionIsRejected() throws Exception {
        ResponseEntity<String> response = restTemplate.postForEntity("/procedures/suggestions",
                json(Map.of("title", "X", "institutionId", UUID.randomUUID().toString(), "details", "Y"), null),
                String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(objectMapper.readTree(response.getBody()).get("error").asText())
                .isEqualTo("invalid-suggestion-payload");
    }

    private static HttpEntity<Map<String, ?>> json(Map<String, ?> body, String bearerToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (bearerToken != null) {
            headers.setBearerAuth(bearerToken);
        }
        return new HttpEntity<>(body, headers);
    }
}
