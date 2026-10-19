package com.darmoz.tramitea.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "tr_procedure_faqs")
public class ProcedureFaq {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "procedure_id")
    private Procedure procedure;

    @Column(nullable = false)
    private String question;

    @Column(nullable = false)
    private String answer;

    @Column(name = "display_order", nullable = false)
    private int displayOrder;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected ProcedureFaq() {
    }

    public UUID getId() { return id; }
    public String getQuestion() { return question; }
    public String getAnswer() { return answer; }
    public int getDisplayOrder() { return displayOrder; }
    public Instant getUpdatedAt() { return updatedAt; }
}
