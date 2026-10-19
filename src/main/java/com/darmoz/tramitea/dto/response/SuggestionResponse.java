package com.darmoz.tramitea.dto.response;

import com.darmoz.tramitea.entity.ProcedureSuggestion;

import java.time.Instant;
import java.util.UUID;

public record SuggestionResponse(UUID id, String title, UUID institutionId, Instant createdAt) {

    public static SuggestionResponse from(ProcedureSuggestion s) {
        return new SuggestionResponse(s.getId(), s.getTitle(), s.getInstitutionId(), s.getCreatedAt());
    }
}
