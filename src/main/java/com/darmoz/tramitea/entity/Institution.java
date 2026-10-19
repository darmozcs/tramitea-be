package com.darmoz.tramitea.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "tr_institutions")
public class Institution {

    @Id
    private UUID id;

    @Column(nullable = false)
    private String acronym;

    @Column(nullable = false)
    private String name;

    @Column(name = "parent_entity")
    private String parentEntity;

    @Column(name = "official_website", nullable = false)
    private String officialWebsite;

    @Column(name = "contact_phone")
    private String contactPhone;

    @Column(name = "contact_email")
    private String contactEmail;

    @Column(name = "logo_url")
    private String logoUrl;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Institution() {
    }

    public UUID getId() { return id; }
    public String getAcronym() { return acronym; }
    public String getName() { return name; }
    public String getParentEntity() { return parentEntity; }
    public String getOfficialWebsite() { return officialWebsite; }
    public String getContactPhone() { return contactPhone; }
    public String getContactEmail() { return contactEmail; }
    public String getLogoUrl() { return logoUrl; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
