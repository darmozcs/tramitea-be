package com.darmoz.tramitea.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * Trámite sugerido por un ciudadano para su provincia o su país. Lo revisa
 * un admin de ese país en tramitea-admin-be (status, reviewed*, procedureId).
 */
@Entity
@Table(name = "tr_procedure_suggestions")
public class ProcedureSuggestion {

    @Id
    private UUID id;

    @Column(nullable = false)
    private String title;

    @Column(name = "institution_id", nullable = false)
    private UUID institutionId;

    @Column(nullable = false)
    private String details;

    @Column(name = "contact_email")
    private String contactEmail;

    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "location_id", nullable = false)
    private UUID locationId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SuggestionStatus status;

    /** Id de darmoz-auth del admin que la revisó. */
    @Column(name = "reviewed_by")
    private UUID reviewedBy;

    @Column(name = "reviewed_at")
    private Instant reviewedAt;

    @Column(name = "rejection_reason")
    private String rejectionReason;

    /** Trámite creado al aceptarla. */
    @Column(name = "procedure_id")
    private UUID procedureId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected ProcedureSuggestion() {
    }

    public ProcedureSuggestion(String title, UUID institutionId, String details, String contactEmail, UUID userId,
                               UUID locationId) {
        this.id = UUID.randomUUID();
        this.title = title;
        this.institutionId = institutionId;
        this.details = details;
        this.contactEmail = contactEmail;
        this.userId = userId;
        this.locationId = locationId;
        this.status = SuggestionStatus.PENDING;
        this.createdAt = Instant.now();
    }

    public UUID getId() { return id; }
    public String getTitle() { return title; }
    public UUID getInstitutionId() { return institutionId; }
    public String getDetails() { return details; }
    public String getContactEmail() { return contactEmail; }
    public UUID getUserId() { return userId; }
    public UUID getLocationId() { return locationId; }
    public SuggestionStatus getStatus() { return status; }
    public UUID getReviewedBy() { return reviewedBy; }
    public Instant getReviewedAt() { return reviewedAt; }
    public String getRejectionReason() { return rejectionReason; }
    public UUID getProcedureId() { return procedureId; }
    public Instant getCreatedAt() { return createdAt; }
}
