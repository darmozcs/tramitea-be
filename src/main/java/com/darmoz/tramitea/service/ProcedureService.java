package com.darmoz.tramitea.service;

import com.darmoz.tramitea.dto.response.CategoryResponse;
import com.darmoz.tramitea.dto.response.InstitutionResponse;
import com.darmoz.tramitea.dto.response.PaginatedResponse;
import com.darmoz.tramitea.dto.response.ProcedureResponse;
import com.darmoz.tramitea.entity.Procedure;
import com.darmoz.tramitea.entity.ProcedureSortBy;
import com.darmoz.tramitea.exception.NotFoundException;
import com.darmoz.tramitea.repository.CategoryRepository;
import com.darmoz.tramitea.repository.InstitutionRepository;
import com.darmoz.tramitea.repository.ProcedureRepository;
import com.darmoz.tramitea.util.TextNormalizer;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Service
public class ProcedureService {

    static final int MAX_PAGE_SIZE = 100;
    static final int MAX_POPULAR_LIMIT = 50;

    private final ProcedureRepository procedureRepository;
    private final CategoryRepository categoryRepository;
    private final InstitutionRepository institutionRepository;

    public ProcedureService(ProcedureRepository procedureRepository, CategoryRepository categoryRepository,
                            InstitutionRepository institutionRepository) {
        this.procedureRepository = procedureRepository;
        this.categoryRepository = categoryRepository;
        this.institutionRepository = institutionRepository;
    }

    /**
     * Misma semantica que el mock del front: una pagina fuera de rango se
     * ajusta a la ultima (o a la primera) en vez de devolver vacio.
     */
    @Transactional(readOnly = true)
    public PaginatedResponse<ProcedureResponse> list(ProcedureFilters filters) {
        Specification<Procedure> spec = toSpecification(filters);
        int pageSize = Math.clamp(filters.pageSize(), 1, MAX_PAGE_SIZE);

        long total = procedureRepository.count(spec);
        int totalPages = (int) Math.max(1, (total + pageSize - 1) / pageSize);
        int currentPage = Math.clamp(filters.page(), 1, totalPages);

        List<ProcedureResponse> items = procedureRepository
                .findAll(spec, PageRequest.of(currentPage - 1, pageSize, toSort(filters.sortBy())))
                .map(ProcedureResponse::from)
                .getContent();

        return new PaginatedResponse<>(total, currentPage, totalPages, items);
    }

    /** Cada consulta del detalle cuenta como una visita (alimenta MOST_VIEWED y populares). */
    @Transactional
    public ProcedureResponse getBySlug(String slug) {
        Procedure procedure = procedureRepository.findBySlug(slug)
                .orElseThrow(() -> new NotFoundException("Tramite no encontrado"));
        procedureRepository.incrementViewCount(procedure.getId());
        return ProcedureResponse.from(procedure);
    }

    @Transactional(readOnly = true)
    public List<ProcedureResponse> getPopular(int limit) {
        return procedureRepository.findPopular(PageRequest.of(0, Math.clamp(limit, 1, MAX_POPULAR_LIMIT))).stream()
                .map(ProcedureResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ProcedureResponse> getByIds(Collection<UUID> ids) {
        if (ids.isEmpty()) {
            return List.of();
        }
        return procedureRepository.findAll((root, query, cb) -> root.get("id").in(ids),
                        Sort.by("displayOrder")).stream()
                .map(ProcedureResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<CategoryResponse> getCategories() {
        return categoryRepository.findByActiveTrueOrderByDisplayOrderAsc().stream()
                .map(CategoryResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<InstitutionResponse> getInstitutions() {
        return institutionRepository.findAll(Sort.by("acronym")).stream()
                .map(InstitutionResponse::from)
                .toList();
    }

    public void requireExists(UUID procedureId) {
        if (!procedureRepository.existsById(procedureId)) {
            throw new NotFoundException("Tramite no encontrado");
        }
    }

    private static Specification<Procedure> toSpecification(ProcedureFilters filters) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            String term = TextNormalizer.normalize(filters.searchTerm()).trim();
            if (!term.isEmpty()) {
                predicates.add(cb.like(root.get("searchText"), "%" + escapeLike(term) + "%", '\\'));
            }
            if (isPresent(filters.categoryIds())) {
                predicates.add(root.get("categoryId").in(filters.categoryIds()));
            }
            if (isPresent(filters.institutionIds())) {
                predicates.add(root.get("institutionId").in(filters.institutionIds()));
            }
            if (isPresent(filters.groups())) {
                predicates.add(root.get("group").in(filters.groups()));
            }
            if (isPresent(filters.methods())) {
                predicates.add(root.get("applicationMethod").in(filters.methods()));
            }
            if (filters.isFreeOnly()) {
                predicates.add(cb.isTrue(root.get("free")));
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }

    /**
     * displayOrder siempre como desempate para que la paginacion sea estable.
     * PRICE_ASC: gratuitos primero y luego por monto; Postgres ordena los
     * NULL al final en ASC, igual que el `Infinity` del mock.
     */
    private static Sort toSort(ProcedureSortBy sortBy) {
        Sort byDisplayOrder = Sort.by("displayOrder");
        if (sortBy == null) {
            return byDisplayOrder;
        }
        return switch (sortBy) {
            case MOST_VIEWED -> Sort.by(Sort.Direction.DESC, "viewCount").and(byDisplayOrder);
            case PRICE_ASC -> Sort.by(Sort.Direction.DESC, "free").and(Sort.by("costAmount")).and(byDisplayOrder);
            case RECENTLY_UPDATED -> Sort.by(Sort.Direction.DESC, "updatedAt").and(byDisplayOrder);
            case RELEVANCE -> byDisplayOrder;
        };
    }

    private static boolean isPresent(Collection<?> values) {
        return values != null && !values.isEmpty();
    }

    private static String escapeLike(String value) {
        return value.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}
