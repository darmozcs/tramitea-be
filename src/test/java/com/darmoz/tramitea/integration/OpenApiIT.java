package com.darmoz.tramitea.integration;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/** El Swagger es lo que se le entrega al front: público y con el candado solo donde hace falta. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class OpenApiIT extends AbstractIntegrationTest {

    @Test
    void apiDocsArePublicAndListTheEndpoints() throws Exception {
        ResponseEntity<String> response = call(HttpMethod.GET, "/v3/api-docs", null, null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        // Copia para entregar sin levantar el servicio (no se versiona: target/).
        Files.writeString(Path.of("target", "openapi.json"),
                objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(body(response)));
        JsonNode paths = body(response).get("paths");
        assertThat(paths.has("/procedures/{slug}")).isTrue();
        assertThat(paths.has("/user/saved-tasks/{taskId}/notes")).isTrue();
        assertThat(body(response).get("servers").get(0).get("url").asText()).isEqualTo("/tramitea-api");
    }

    @Test
    void onlyAuthenticatedOperationsRequireBearer() throws Exception {
        JsonNode paths = body(call(HttpMethod.GET, "/v3/api-docs", null, null)).get("paths");

        assertThat(paths.get("/user/profile").get("get").has("security")).isTrue();
        assertThat(paths.get("/procedures/{slug}").get("get").has("security")).isFalse();
        assertThat(paths.get("/user/profile").get("get").get("parameters")).isNull();
    }

    @Test
    void departmentIsDocumentedWithItsTwoLetterCode() throws Exception {
        JsonNode schemas = body(call(HttpMethod.GET, "/v3/api-docs", null, null)).get("components").get("schemas");

        assertThat(schemas.get("UpdateProfile").get("properties").get("department").get("enum").toString())
                .contains("\"MO\"");
    }

    @Test
    void swaggerUiIsPublic() {
        ResponseEntity<String> response = call(HttpMethod.GET, "/swagger-ui/index.html", null, null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }
}
