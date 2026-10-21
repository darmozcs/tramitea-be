package com.darmoz.tramitea.controller;

import com.darmoz.tramitea.dto.request.Requests;
import com.darmoz.tramitea.dto.response.CategoryResponse;
import com.darmoz.tramitea.dto.response.CommunityCommentResponse;
import com.darmoz.tramitea.dto.response.InstitutionResponse;
import com.darmoz.tramitea.dto.response.PaginatedResponse;
import com.darmoz.tramitea.dto.response.ProcedureResponse;
import com.darmoz.tramitea.dto.response.SuggestionResponse;
import com.darmoz.tramitea.entity.ApplicationMethod;
import com.darmoz.tramitea.entity.ProcedureGroup;
import com.darmoz.tramitea.entity.ProcedureSortBy;
import com.darmoz.tramitea.security.AuthenticatedUser;
import com.darmoz.tramitea.service.CommunityService;
import com.darmoz.tramitea.service.ProcedureFilters;
import com.darmoz.tramitea.service.ProcedureService;
import com.darmoz.tramitea.service.SuggestionService;
import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * Catalogo y comunidad. Las rutas literales (/popular, /categories...) le
 * ganan a /{slug} en el matching de Spring MVC.
 */
@Tag(name = "Trámites", description = "Catálogo de trámites y comunidad. La lectura es pública.")
@RestController
@RequestMapping("/procedures")
public class ProcedureController {

    // GET /procedures y GET /procedures?ids= comparten path: springdoc los junta en una operación.
    private static final String LIST_SUMMARY = "Listar trámites";
    private static final String LIST_DESCRIPTION = "Paginado y con filtros. Las listas van separadas por coma (`?groups=IDENTITY,HEALTH`). `page` empieza en 1; `pageSize` de 1 a 100. Con `?ids=a,b` devuelve en cambio un array con esos trámites, sin paginar, e ignora los demás filtros.";

    private final ProcedureService procedureService;
    private final CommunityService communityService;
    private final SuggestionService suggestionService;

    public ProcedureController(ProcedureService procedureService, CommunityService communityService,
                               SuggestionService suggestionService) {
        this.procedureService = procedureService;
        this.communityService = communityService;
        this.suggestionService = suggestionService;
    }

    /** Listas como valores separados por coma: ?categoryIds=a,b&groups=IDENTITY,HEALTH */
    @GetMapping
    @Operation(summary = LIST_SUMMARY, description = LIST_DESCRIPTION)
    public PaginatedResponse<ProcedureResponse> list(
            @RequestParam(required = false) String searchTerm,
            @RequestParam(required = false) List<UUID> categoryIds,
            @RequestParam(required = false) List<UUID> institutionIds,
            @RequestParam(required = false) List<ProcedureGroup> groups,
            @RequestParam(required = false) List<ApplicationMethod> methods,
            @RequestParam(defaultValue = "false") boolean isFreeOnly,
            @RequestParam(defaultValue = "RELEVANCE") ProcedureSortBy sortBy,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "12") int pageSize) {
        return procedureService.list(new ProcedureFilters(searchTerm, categoryIds, institutionIds, groups, methods,
                isFreeOnly, sortBy, page, pageSize));
    }

    @GetMapping(params = "ids")
    @Operation(summary = LIST_SUMMARY, description = LIST_DESCRIPTION)
    public List<ProcedureResponse> getByIds(@RequestParam List<UUID> ids) {
        return procedureService.getByIds(ids);
    }

    @GetMapping("/popular")
    @Operation(summary = "Trámites más vistos", description = "`limit` de 1 a 50.")
    public List<ProcedureResponse> getPopular(@RequestParam(defaultValue = "6") int limit) {
        return procedureService.getPopular(limit);
    }

    @GetMapping("/categories")
    @Operation(summary = "Categorías")
    public List<CategoryResponse> getCategories() {
        return procedureService.getCategories();
    }

    @GetMapping("/institutions")
    @Operation(summary = "Instituciones")
    public List<InstitutionResponse> getInstitutions() {
        return procedureService.getInstitutions();
    }

    @GetMapping("/{slug}")
    @Operation(summary = "Detalle de un trámite", description = "Acepta el slug en español o en inglés. 404 si no existe.")
    public ProcedureResponse getBySlug(@PathVariable String slug) {
        return procedureService.getBySlug(slug);
    }

    @PostMapping("/suggestions")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Sugerir un trámite", description = "El token es opcional: sin token la sugerencia es anónima. `title` hasta 255, `details` hasta 5000, `institutionId` tiene que existir.")
    public SuggestionResponse suggest(@RequestBody Requests.SuggestProcedure payload,
                                      @AuthenticationPrincipal AuthenticatedUser user) {
        return suggestionService.suggest(payload, user);
    }

    @GetMapping("/{procedureId}/comments")
    @Operation(summary = "Comentarios de un trámite")
    public List<CommunityCommentResponse> getComments(@PathVariable UUID procedureId) {
        return communityService.getComments(procedureId);
    }

    @PostMapping("/{procedureId}/comments")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Comentar o responder", description = "`content` hasta 2000. `parentId` opcional, de un comentario del mismo trámite.")
    public CommunityCommentResponse postComment(@PathVariable UUID procedureId,
                                                @Valid @RequestBody Requests.CreateComment payload,
                                                @AuthenticationPrincipal AuthenticatedUser user) {
        return communityService.postComment(procedureId, payload.content(), payload.parentId(), user);
    }

    @GetMapping("/{procedureId}/usefulness")
    @Operation(summary = "Votos de guía útil", description = "Cantidad de votos `USEFUL`.")
    public long getUsefulness(@PathVariable UUID procedureId) {
        return communityService.getGuideUsefulness(procedureId);
    }

    @PostMapping("/{procedureId}/vote")
    @Operation(summary = "Votar la guía", description = "Un voto por usuario; si vuelve a votar se reemplaza. Devuelve la cantidad de votos `USEFUL`.")
    public long vote(@PathVariable UUID procedureId, @Valid @RequestBody Requests.SubmitVote payload,
                     @AuthenticationPrincipal AuthenticatedUser user) {
        return communityService.submitGuideVote(procedureId, payload.value(), user);
    }
}
