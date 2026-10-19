package com.darmoz.tramitea.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "tr_saved_tasks")
public class SavedTask {

    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "procedure_id", nullable = false)
    private UUID procedureId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SavedTaskStatus status;

    private String notes;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected SavedTask() {
    }

    public SavedTask(UUID userId, UUID procedureId) {
        this.id = UUID.randomUUID();
        this.userId = userId;
        this.procedureId = procedureId;
        this.status = SavedTaskStatus.PENDING;
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
    }

    public void changeStatus(SavedTaskStatus status) {
        this.status = status;
        this.updatedAt = Instant.now();
        this.completedAt = status == SavedTaskStatus.COMPLETED ? this.updatedAt : null;
    }

    public void changeNotes(String notes) {
        this.notes = notes;
        this.updatedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getUserId() { return userId; }
    public UUID getProcedureId() { return procedureId; }
    public SavedTaskStatus getStatus() { return status; }
    public String getNotes() { return notes; }
    public Instant getCompletedAt() { return completedAt; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
