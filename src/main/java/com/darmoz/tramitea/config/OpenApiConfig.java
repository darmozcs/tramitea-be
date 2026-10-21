package com.darmoz.tramitea.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

import java.util.Arrays;
import java.util.List;

/**
 * Swagger UI en /tramitea-api/swagger-ui.html y el JSON en
 * /tramitea-api/v3/api-docs (se apagan con API_DOCS_ENABLED=false).
 */
@Configuration
public class OpenApiConfig {

    static final String BEARER = "bearer";

    @Bean
    public OpenAPI tramiteaOpenApi(@Value("${auth.mode}") String authMode) {
        String tokenSource = AuthMode.DARMOZ.equals(authMode)
                ? "Este ambiente usa darmoz-auth: el token es el `accessToken` de `POST /auth/login` de darmoz-auth"
                        + " (header `API_ID` de la app Tramitea). Dura 12 minutos."
                : "Este ambiente usa auth local: el token es el `token` de `POST /auth/sign-in`.";
        return new OpenAPI()
                .info(new Info()
                        .title("Tramitea API")
                        .version("v1")
                        .description("""
                                Backend de Tramitea: catálogo de trámites, comunidad, perfil y tareas guardadas.

                                Autenticación: header `Authorization: Bearer <token>` en los endpoints con candado. \
                                %s

                                Errores: `{ timestamp, status, error, message }`; `error` lleva un código estable \
                                (`invalid-profile-payload`, `email-already-registered`...) cuando lo hay. Fechas \
                                ISO-8601 en UTC; los campos null se omiten.""".formatted(tokenSource)))
                // Relativo: Swagger UI lo resuelve contra el host desde el que se abre (Traefik, Railway, local).
                .servers(List.of(new Server().url("/tramitea-api")))
                .components(new Components().addSecuritySchemes(BEARER, new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")));
    }

    /** Candado solo en los endpoints que reciben el usuario autenticado. */
    @Bean
    public OperationCustomizer bearerOnAuthenticatedOperations() {
        return (operation, handlerMethod) -> {
            boolean usesPrincipal = Arrays.stream(handlerMethod.getMethodParameters())
                    .anyMatch(p -> p.hasParameterAnnotation(AuthenticationPrincipal.class));
            if (usesPrincipal) {
                operation.addSecurityItem(new SecurityRequirement().addList(BEARER));
            }
            return operation;
        };
    }
}
