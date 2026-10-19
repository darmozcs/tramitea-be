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
@RestController
@RequestMapping("/procedures")
public class ProcedureController {

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
    public List<ProcedureResponse> getByIds(@RequestParam List<UUID> ids) {
        return procedureService.getByIds(ids);
    }

    @GetMapping("/popular")
    public List<ProcedureResponse> getPopular(@RequestParam(defaultValue = "6") int limit) {
        return procedureService.getPopular(limit);
    }

    @GetMapping("/categories")
    public List<CategoryResponse> getCategories() {
        return procedureService.getCategories();
    }

    @GetMapping("/institutions")
    public List<InstitutionResponse> getInstitutions() {
        return procedureService.getInstitutions();
    }

    @GetMapping("/{slug}")
    public ProcedureResponse getBySlug(@PathVariable String slug) {
        return procedureService.getBySlug(slug);
    }

    @PostMapping("/suggestions")
    @ResponseStatus(HttpStatus.CREATED)
    public SuggestionResponse suggest(@RequestBody Requests.SuggestProcedure payload,
                                      @AuthenticationPrincipal AuthenticatedUser user) {
        return suggestionService.suggest(payload, user);
    }

    @GetMapping("/{procedureId}/comments")
    public List<CommunityCommentResponse> getComments(@PathVariable UUID procedureId) {
        return communityService.getComments(procedureId);
    }

    @PostMapping("/{procedureId}/comments")
    @ResponseStatus(HttpStatus.CREATED)
    public CommunityCommentResponse postComment(@PathVariable UUID procedureId,
                                                @Valid @RequestBody Requests.CreateComment payload,
                                                @AuthenticationPrincipal AuthenticatedUser user) {
        return communityService.postComment(procedureId, payload.content(), payload.parentId(), user);
    }

    @GetMapping("/{procedureId}/usefulness")
    public long getUsefulness(@PathVariable UUID procedureId) {
        return communityService.getGuideUsefulness(procedureId);
    }

    @PostMapping("/{procedureId}/vote")
    public long vote(@PathVariable UUID procedureId, @Valid @RequestBody Requests.SubmitVote payload,
                     @AuthenticationPrincipal AuthenticatedUser user) {
        return communityService.submitGuideVote(procedureId, payload.value(), user);
    }
}
