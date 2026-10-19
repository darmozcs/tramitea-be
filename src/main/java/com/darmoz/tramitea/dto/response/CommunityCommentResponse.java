package com.darmoz.tramitea.dto.response;

import com.darmoz.tramitea.entity.CommunityComment;

import java.time.Instant;
import java.util.UUID;

public record CommunityCommentResponse(UUID id, UUID procedureId, UUID parentId, Author author, String content,
                                       int upvotes, int downvotes, Instant createdAt) {

    public static CommunityCommentResponse from(CommunityComment c) {
        return new CommunityCommentResponse(c.getId(), c.getProcedureId(), c.getParentId(),
                new Author(c.getAuthorDisplayName(), c.getAuthorAvatarUrl(), c.isAuthorVerified()),
                c.getContent(), c.getUpvotes(), c.getDownvotes(), c.getCreatedAt());
    }

    public record Author(String displayName, String avatarUrl, boolean isVerifiedCitizen) {
    }
}
