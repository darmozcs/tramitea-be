package com.darmoz.tramitea.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Catalogo de solo lectura desde la API (se carga por migraciones). Lo
 * unico que cambia en runtime es viewCount.
 */
@Entity
@Table(name = "tr_procedures")
public class Procedure {

    @Id
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "application_method", nullable = false)
    private ApplicationMethod applicationMethod;

    @Enumerated(EnumType.STRING)
    @Column(name = "authentication_level", nullable = false)
    private AuthenticationLevel authenticationLevel;

    @Enumerated(EnumType.STRING)
    @Column(name = "procedure_group", nullable = false)
    private ProcedureGroup group;

    @Column(nullable = false)
    private String description;

    @Column(name = "search_text", nullable = false)
    private String searchText;

    @Column(name = "image_url", nullable = false)
    private String imageUrl;

    @Column(name = "category_id", nullable = false)
    private UUID categoryId;

    @Column(name = "institution_id", nullable = false)
    private UUID institutionId;

    @Column(name = "appointment_required", nullable = false)
    private boolean appointmentRequired;

    @Column(name = "is_free", nullable = false)
    private boolean free;

    @Column(name = "cost_amount")
    private BigDecimal costAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "cost_currency")
    private Currency costCurrency;

    @Column(name = "cost_comments")
    private String costComments;

    @Column(name = "validity_value")
    private String validityValue;

    @Column(name = "validity_unit")
    private String validityUnit;

    @Column(name = "validity_comments")
    private String validityComments;

    @Column(name = "estimated_delivery_time")
    private String estimatedDeliveryTime;

    @Column(name = "view_count", nullable = false)
    private long viewCount;

    @Column(name = "slug_es", nullable = false)
    private String slugEs;

    @Column(name = "slug_en", nullable = false)
    private String slugEn;

    @Column(name = "display_order", nullable = false)
    private int displayOrder;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @OneToMany(mappedBy = "procedure")
    @OrderBy("displayOrder ASC")
    private List<Requirement> requirements = new ArrayList<>();

    @OneToMany(mappedBy = "procedure")
    @OrderBy("stepNumber ASC")
    private List<ProcedureStep> steps = new ArrayList<>();

    @OneToMany(mappedBy = "procedure")
    @OrderBy("displayOrder ASC")
    private List<ProcedureFaq> faqs = new ArrayList<>();

    protected Procedure() {
    }

    public UUID getId() { return id; }
    public String getName() { return name; }
    public ApplicationMethod getApplicationMethod() { return applicationMethod; }
    public AuthenticationLevel getAuthenticationLevel() { return authenticationLevel; }
    public ProcedureGroup getGroup() { return group; }
    public String getDescription() { return description; }
    public String getImageUrl() { return imageUrl; }
    public UUID getCategoryId() { return categoryId; }
    public UUID getInstitutionId() { return institutionId; }
    public boolean isAppointmentRequired() { return appointmentRequired; }
    public boolean isFree() { return free; }
    public BigDecimal getCostAmount() { return costAmount; }
    public Currency getCostCurrency() { return costCurrency; }
    public String getCostComments() { return costComments; }
    public String getValidityValue() { return validityValue; }
    public String getValidityUnit() { return validityUnit; }
    public String getValidityComments() { return validityComments; }
    public String getEstimatedDeliveryTime() { return estimatedDeliveryTime; }
    public long getViewCount() { return viewCount; }
    public String getSlugEs() { return slugEs; }
    public String getSlugEn() { return slugEn; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public List<Requirement> getRequirements() { return requirements; }
    public List<ProcedureStep> getSteps() { return steps; }
    public List<ProcedureFaq> getFaqs() { return faqs; }
}
