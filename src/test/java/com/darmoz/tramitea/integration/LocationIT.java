package com.darmoz.tramitea.integration;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/** Ubicaciones sembradas por V4__locations.sql: Uruguay activo, Brasil y España no. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class LocationIT extends AbstractIntegrationTest {

    static final String INSTITUTION_ID = CommunityIT.INSTITUTION_ID;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void onlyVisibleLocationsAreListed() throws Exception {
        JsonNode locations = body(call(HttpMethod.GET, "/locations", null, null));

        assertThat(locations).hasSize(20);
        assertThat(codes(locations)).contains("UY", "UY-MO", "UY-TT").doesNotContain("BR", "BR-SP", "ES", "ES-M");
        JsonNode montevideo = find(locations, "UY-MO");
        assertThat(montevideo.get("type").asText()).isEqualTo("PROVINCE");
        assertThat(montevideo.get("parentId").asText()).isEqualTo(locationId("UY").toString());
    }

    @Test
    void provinceFilterIncludesNationalProceduresOfItsCountry() throws Exception {
        JsonNode all = body(call(HttpMethod.GET, "/procedures?pageSize=1", null, null));
        assertThat(all.get("items").get(0).get("locationId").asText()).isEqualTo(locationId("UY").toString());

        JsonNode montevideo = body(call(HttpMethod.GET, "/procedures?locationIds=" + locationId("UY-MO"), null, null));
        assertThat(montevideo.get("total").asInt()).isEqualTo(14);

        JsonNode brazil = body(call(HttpMethod.GET, "/procedures?locationIds=" + locationId("BR"), null, null));
        assertThat(brazil.get("total").asInt()).isZero();
    }

    @Test
    void proceduresOfDisabledCountryAreHidden() throws Exception {
        jdbcTemplate.update("update tr_locations set is_active = false where code = 'UY'");
        try {
            assertThat(body(call(HttpMethod.GET, "/procedures", null, null)).get("total").asInt()).isZero();
            assertThat(call(HttpMethod.GET, "/procedures/cedula-de-identidad", null, null).getStatusCode())
                    .isEqualTo(HttpStatus.NOT_FOUND);
            assertThat(body(call(HttpMethod.GET, "/procedures/popular", null, null))).isEmpty();
            assertThat(body(call(HttpMethod.GET, "/locations", null, null))).isEmpty();
        } finally {
            jdbcTemplate.update("update tr_locations set is_active = true where code = 'UY'");
        }
    }

    @Test
    void userWithoutLocationCannotSuggest() throws Exception {
        String token = registerUser("Sinlugar");

        ResponseEntity<String> response = call(HttpMethod.POST, "/procedures/suggestions", suggestion(null), token);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(errorCode(response)).isEqualTo("location-required");
    }

    @Test
    void userSuggestsForItsProvinceOrItsCountryOnly() throws Exception {
        String token = registerUser("Montevideana");
        setLocation(token, locationId("UY-MO"));

        JsonNode byDefault = body(call(HttpMethod.POST, "/procedures/suggestions", suggestion(null), token));
        assertThat(byDefault.get("locationId").asText()).isEqualTo(locationId("UY-MO").toString());
        assertThat(byDefault.get("status").asText()).isEqualTo("PENDING");

        JsonNode national = body(call(HttpMethod.POST, "/procedures/suggestions", suggestion(locationId("UY")), token));
        assertThat(national.get("locationId").asText()).isEqualTo(locationId("UY").toString());

        for (String other : new String[]{"UY-CA", "BR", "BR-SP"}) {
            ResponseEntity<String> response = call(HttpMethod.POST, "/procedures/suggestions",
                    suggestion(locationId(other)), token);
            assertThat(response.getStatusCode()).as(other).isEqualTo(HttpStatus.FORBIDDEN);
            assertThat(errorCode(response)).isEqualTo("location-not-allowed");
        }
    }

    @Test
    void profileOnlyAcceptsVisibleLocationsAndKeepsItWhenOmitted() throws Exception {
        String token = registerUser("Viajera");
        setLocation(token, locationId("UY-SA"));

        ResponseEntity<String> disabled = call(HttpMethod.PATCH, "/user/profile",
                profilePayload(token, locationId("BR-SP")), token);
        assertThat(disabled.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);

        JsonNode kept = body(call(HttpMethod.PATCH, "/user/profile", profilePayload(token, null), token));
        assertThat(kept.get("locationId").asText()).isEqualTo(locationId("UY-SA").toString());
    }

    private void setLocation(String token, UUID locationId) throws Exception {
        JsonNode profile = body(call(HttpMethod.PATCH, "/user/profile", profilePayload(token, locationId), token));
        assertThat(profile.get("locationId").asText()).isEqualTo(locationId.toString());
    }

    private Map<String, Object> profilePayload(String token, UUID locationId) throws Exception {
        JsonNode profile = body(call(HttpMethod.GET, "/user/profile", null, token));
        Map<String, Object> payload = new HashMap<>();
        payload.put("firstName", profile.get("firstName").asText());
        payload.put("lastName", profile.get("lastName").asText());
        payload.put("email", profile.get("email").asText());
        payload.put("locationId", locationId);
        return payload;
    }

    private static Map<String, Object> suggestion(UUID locationId) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("title", "Permiso de pesca");
        payload.put("institutionId", INSTITUTION_ID);
        payload.put("details", "Falta este tramite");
        payload.put("locationId", locationId);
        return payload;
    }

    private UUID locationId(String code) {
        return jdbcTemplate.queryForObject("select id from tr_locations where code = ?", UUID.class, code);
    }

    private static List<String> codes(JsonNode locations) {
        List<String> codes = new ArrayList<>();
        locations.forEach(l -> codes.add(l.get("code").asText()));
        return codes;
    }

    private static JsonNode find(JsonNode locations, String code) {
        for (JsonNode location : locations) {
            if (location.get("code").asText().equals(code)) {
                return location;
            }
        }
        throw new AssertionError("No esta " + code);
    }
}
