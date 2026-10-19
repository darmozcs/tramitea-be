package com.darmoz.tramitea.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * NOTA: el contenedor de Postgres se arranca a mano en un bloque estatico y
 * nunca se detiene explicitamente ("singleton container pattern"). Con
 * @Container, la extension de JUnit5 lo para al terminar la PRIMERA clase
 * *IT y Spring sigue usando el ApplicationContext cacheado con el
 * DataSource apuntando al container viejo -> Connection refused en la
 * siguiente. Arrancarlo una sola vez y dejar que Ryuk lo mate al terminar
 * la JVM evita el problema.
 */
@ActiveProfiles("test")
public abstract class AbstractIntegrationTest {

    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    static final String TEST_SECRET = "tramitea-be-integration-test-secret-0123456789";

    static {
        POSTGRES.start();
    }

    @Autowired
    protected TestRestTemplate restTemplate;

    @Autowired
    protected ObjectMapper objectMapper;

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("auth.secret", () -> TEST_SECRET);
    }

    /** Registra un usuario nuevo (email unico) y devuelve su token de sesion. */
    protected String registerUser(String firstName) throws Exception {
        String email = firstName.toLowerCase() + "-" + UUID.randomUUID() + "@example.com";
        ResponseEntity<String> response = call(HttpMethod.POST, "/auth/register", Map.of(
                "firstName", firstName, "lastName", "Prueba", "email", email, "password", "secreta-123"), null);
        return body(response).get("token").asText();
    }

    protected ResponseEntity<String> call(HttpMethod method, String path, Object body, String bearerToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (bearerToken != null) {
            headers.setBearerAuth(bearerToken);
        }
        return restTemplate.exchange(path, method, new HttpEntity<>(body, headers), String.class);
    }

    /** Body JSON de una respuesta 2xx (falla mostrando status y body si no lo es). */
    protected JsonNode body(ResponseEntity<String> response) throws Exception {
        assertThat(response.getStatusCode().is2xxSuccessful())
                .as("%s -> %s", response.getStatusCode(), response.getBody()).isTrue();
        return objectMapper.readTree(response.getBody());
    }

    protected String errorCode(ResponseEntity<String> response) throws Exception {
        return objectMapper.readTree(response.getBody()).get("error").asText();
    }
}
