package com.darmoz.tramitea.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "tr_community_comments")
public class CommunityComment {

    public static final String DELETED_AUTHOR_NAME = "Usuario eliminado";

    @Id
    private UUID id;

    @Column(name = "procedure_id", nullable = false)
    private UUID procedureId;

    @Column(name = "parent_id")
    private UUID parentId;

    /** Null en comentarios sembrados o de cuentas eliminadas. */
    @Column(name = "author_user_id")
    private UUID authorUserId;

    @Column(name = "author_display_name", nullable = false)
    private String authorDisplayName;

    @Column(name = "author_avatar_url")
    private String authorAvatarUrl;

    @Column(name = "author_verified", nullable = false)
    private boolean authorVerified;

    @Column(nullable = false)
    private String content;

    @Column(nullable = false)
    private int upvotes;

    @Column(nullable = false)
    private int downvotes;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected CommunityComment() {
    }

    public CommunityComment(UUID procedureId, UUID parentId, CitizenUser author, String content) {
        this.id = UUID.randomUUID();
        this.procedureId = procedureId;
        this.parentId = parentId;
        this.authorUserId = author.getId();
        this.authorDisplayName = author.getDisplayName();
        this.authorVerified = author.isNationalIdVerified();
        this.content = content;
        this.createdAt = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getProcedureId() { return procedureId; }
    public UUID getParentId() { return parentId; }
    public UUID getAuthorUserId() { return authorUserId; }
    public String getAuthorDisplayName() { return authorDisplayName; }
    public String getAuthorAvatarUrl() { return authorAvatarUrl; }
    public boolean isAuthorVerified() { return authorVerified; }
    public String getContent() { return content; }
    public int getUpvotes() { return upvotes; }
    public int getDownvotes() { return downvotes; }
    public Instant getCreatedAt() { return createdAt; }
}
