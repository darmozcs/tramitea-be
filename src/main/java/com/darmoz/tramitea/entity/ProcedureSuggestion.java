package com.darmoz.tramitea.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

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

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected ProcedureSuggestion() {
    }

    public ProcedureSuggestion(String title, UUID institutionId, String details, String contactEmail, UUID userId) {
        this.id = UUID.randomUUID();
        this.title = title;
        this.institutionId = institutionId;
        this.details = details;
        this.contactEmail = contactEmail;
        this.userId = userId;
        this.createdAt = Instant.now();
    }

    public UUID getId() { return id; }
    public String getTitle() { return title; }
    public UUID getInstitutionId() { return institutionId; }
    public String getDetails() { return details; }
    public String getContactEmail() { return contactEmail; }
    public UUID getUserId() { return userId; }
    public Instant getCreatedAt() { return createdAt; }
}
