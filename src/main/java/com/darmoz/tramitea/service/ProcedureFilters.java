package com.darmoz.tramitea.service;

import com.darmoz.tramitea.entity.ApplicationMethod;
import com.darmoz.tramitea.entity.ProcedureGroup;
import com.darmoz.tramitea.entity.ProcedureSortBy;

import java.util.List;
import java.util.UUID;

/**
 * Espejo de `ProceduresFiltersPayload` del front. `department` existe en el
 * contrato pero el front todavia no filtra por el, asi que no se recibe.
 */
public record ProcedureFilters(String searchTerm, List<UUID> categoryIds, List<UUID> institutionIds,
                               List<ProcedureGroup> groups, List<ApplicationMethod> methods, boolean isFreeOnly,
                               ProcedureSortBy sortBy, int page, int pageSize) {
}
