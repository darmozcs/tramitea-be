package com.darmoz.tramitea.dto.response;

import com.darmoz.tramitea.entity.ProcedureSuggestion;
import com.darmoz.tramitea.entity.SuggestionStatus;

import java.time.Instant;
import java.util.UUID;

public record SuggestionResponse(UUID id, String title, UUID institutionId, UUID locationId, SuggestionStatus status,
                                 Instant createdAt) {

    public static SuggestionResponse from(ProcedureSuggestion s) {
        return new SuggestionResponse(s.getId(), s.getTitle(), s.getInstitutionId(), s.getLocationId(),
                s.getStatus(), s.getCreatedAt());
    }
}
