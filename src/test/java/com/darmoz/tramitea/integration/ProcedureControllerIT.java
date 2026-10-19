package com.darmoz.tramitea.integration;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** Corre contra el catalogo sembrado por V2__seed_catalog.sql (14 tramites, 7 gratuitos). */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ProcedureControllerIT extends AbstractIntegrationTest {

    static final String CEDULA_ID = "cccccccc-cccc-4ccc-8ccc-000000000001";
    static final String IDENTITY_CATEGORY_ID = "aaaaaaaa-aaaa-4aaa-8aaa-000000000001";


    @Test
    void listReturnsFirstPageInEditorialOrder() throws Exception {
        JsonNode body = getJson("/procedures?pageSize=5");

        assertThat(body.get("total").asInt()).isEqualTo(14);
        assertThat(body.get("totalPages").asInt()).isEqualTo(3);
        assertThat(body.get("currentPage").asInt()).isEqualTo(1);
        assertThat(body.get("items")).hasSize(5);
        assertThat(body.get("items").get(0).get("id").asText()).isEqualTo(CEDULA_ID);
    }

    @Test
    void procedureJsonMatchesFrontendContract() throws Exception {
        JsonNode cedula = getJson("/procedures?pageSize=1").get("items").get(0);

        assertThat(cedula.get("isFree").asBoolean()).isTrue();
        assertThat(cedula.get("group").asText()).isEqualTo("IDENTITY");
        assertThat(cedula.get("slug").get("es").asText()).isEqualTo("cedula-de-identidad");
        assertThat(cedula.get("slug").get("en").asText()).isEqualTo("identity-card");
        assertThat(cedula.has("cost")).isFalse();
        assertThat(cedula.get("validity").get("unit").asText()).isEqualTo("años");
        assertThat(cedula.get("requirements").get(0).get("isRequired").asBoolean()).isTrue();
        assertThat(cedula.get("requirements").get(0).get("procedureId").asText()).isEqualTo(CEDULA_ID);
        assertThat(cedula.get("steps")).isNotEmpty();
        assertThat(cedula.get("createdAt").asText()).endsWith("Z");
    }

    @Test
    void searchIsAccentAndCaseInsensitive() throws Exception {
        JsonNode body = getJson("/procedures?searchTerm=CEDULA");

        assertThat(ids(body.get("items"))).contains(CEDULA_ID);
    }

    @Test
    void filtersCombine() throws Exception {
        JsonNode byCategory = getJson("/procedures?pageSize=100&categoryIds=" + IDENTITY_CATEGORY_ID);
        byCategory.get("items").forEach(p -> assertThat(p.get("categoryId").asText()).isEqualTo(IDENTITY_CATEGORY_ID));

        JsonNode freeOnly = getJson("/procedures?pageSize=100&isFreeOnly=true");
        assertThat(freeOnly.get("total").asInt()).isEqualTo(7);
        freeOnly.get("items").forEach(p -> assertThat(p.get("isFree").asBoolean()).isTrue());

        JsonNode groups = getJson("/procedures?pageSize=100&groups=IDENTITY,VEHICLES&methods=IN_PERSON");
        groups.get("items").forEach(p -> {
            assertThat(p.get("group").asText()).isIn("IDENTITY", "VEHICLES");
            assertThat(p.get("applicationMethod").asText()).isEqualTo("IN_PERSON");
        });
    }

    @Test
    void priceAscPutsFreeFirstThenCheapest() throws Exception {
        JsonNode items = getJson("/procedures?pageSize=100&sortBy=PRICE_ASC").get("items");

        for (int i = 0; i < 7; i++) {
            assertThat(items.get(i).get("isFree").asBoolean()).isTrue();
        }
        List<Double> amounts = new ArrayList<>();
        for (int i = 7; i < items.size(); i++) {
            amounts.add(items.get(i).get("cost").get("amount").asDouble());
        }
        assertThat(amounts).isSorted().first().isEqualTo(890.0);
    }

    @Test
    void outOfRangePageIsClampedToLastPage() throws Exception {
        JsonNode body = getJson("/procedures?pageSize=5&page=99");

        assertThat(body.get("currentPage").asInt()).isEqualTo(3);
        assertThat(body.get("items")).hasSize(4);
    }

    @Test
    void getByIdsSkipsUnknownIds() throws Exception {
        JsonNode body = getJson("/procedures?ids=" + CEDULA_ID + ",00000000-0000-0000-0000-000000000000");

        assertThat(body.isArray()).isTrue();
        assertThat(ids(body)).containsExactly(CEDULA_ID);
    }

    @Test
    void popularIsSortedByViewsDesc() throws Exception {
        JsonNode body = getJson("/procedures/popular?limit=4");

        assertThat(body).hasSize(4);
        List<Long> views = new ArrayList<>();
        body.forEach(p -> views.add(p.get("viewCount").asLong()));
        assertThat(views).isSortedAccordingTo((a, b) -> Long.compare(b, a));
    }

    @Test
    void taxonomies() throws Exception {
        JsonNode categories = getJson("/procedures/categories");
        assertThat(categories).hasSize(8);
        assertThat(categories.get(0).get("displayOrder").asInt()).isEqualTo(1);
        assertThat(categories.get(0).get("isActive").asBoolean()).isTrue();

        assertThat(getJson("/procedures/institutions")).hasSize(10);
    }

    @Test
    void getBySlugAcceptsBothLocalesAndCountsViews() throws Exception {
        long before = getJson("/procedures/cedula-de-identidad").get("viewCount").asLong();
        JsonNode english = getJson("/procedures/identity-card");

        assertThat(english.get("id").asText()).isEqualTo(CEDULA_ID);
        assertThat(english.get("viewCount").asLong()).isGreaterThan(before);
    }

    @Test
    void unknownSlugIs404() {
        ResponseEntity<String> response = restTemplate.getForEntity("/procedures/no-existe", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void invalidEnumIs400() {
        ResponseEntity<String> response = restTemplate.getForEntity("/procedures?sortBy=CHEAPEST", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    private JsonNode getJson(String path) throws Exception {
        ResponseEntity<String> response = restTemplate.getForEntity(path, String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        return objectMapper.readTree(response.getBody());
    }

    private static List<String> ids(JsonNode array) {
        List<String> ids = new ArrayList<>();
        array.forEach(p -> ids.add(p.get("id").asText()));
        return ids;
    }
}
