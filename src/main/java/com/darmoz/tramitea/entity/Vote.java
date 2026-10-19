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
@Table(name = "tr_votes")
public class Vote {

    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "procedure_id", nullable = false)
    private UUID procedureId;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_type", nullable = false)
    private VoteTargetType targetType;

    @Column(name = "target_id", nullable = false)
    private UUID targetId;

    @Enumerated(EnumType.STRING)
    @Column(name = "vote_value", nullable = false)
    private VoteValue value;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected Vote() {
    }

    public Vote(UUID userId, UUID procedureId, VoteTargetType targetType, UUID targetId, VoteValue value) {
        this.id = UUID.randomUUID();
        this.userId = userId;
        this.procedureId = procedureId;
        this.targetType = targetType;
        this.targetId = targetId;
        this.value = value;
        this.createdAt = Instant.now();
    }

    public void changeValue(VoteValue value) {
        this.value = value;
        this.createdAt = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getUserId() { return userId; }
    public UUID getProcedureId() { return procedureId; }
    public VoteTargetType getTargetType() { return targetType; }
    public UUID getTargetId() { return targetId; }
    public VoteValue getValue() { return value; }
    public Instant getCreatedAt() { return createdAt; }
}
