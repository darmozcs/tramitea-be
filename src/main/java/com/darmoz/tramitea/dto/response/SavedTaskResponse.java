package com.darmoz.tramitea.dto.response;

import com.darmoz.tramitea.entity.SavedTask;
import com.darmoz.tramitea.entity.SavedTaskStatus;

import java.time.Instant;
import java.util.UUID;

public record SavedTaskResponse(UUID id, UUID userId, UUID procedureId, SavedTaskStatus status, String notes,
                                Instant completedAt, Instant createdAt, Instant updatedAt) {

    public static SavedTaskResponse from(SavedTask t) {
        return new SavedTaskResponse(t.getId(), t.getUserId(), t.getProcedureId(), t.getStatus(), t.getNotes(),
                t.getCompletedAt(), t.getCreatedAt(), t.getUpdatedAt());
    }
}
