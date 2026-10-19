package com.darmoz.tramitea.repository;

import com.darmoz.tramitea.entity.CommunityComment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface CommunityCommentRepository extends JpaRepository<CommunityComment, UUID> {

    List<CommunityComment> findByProcedureIdOrderByCreatedAtAsc(UUID procedureId);

    boolean existsByIdAndProcedureId(UUID id, UUID procedureId);

    /** Mantiene el snapshot de autor alineado cuando el usuario cambia su nombre. */
    @Modifying
    @Query("update CommunityComment c set c.authorDisplayName = :displayName where c.authorUserId = :userId")
    void renameAuthor(UUID userId, String displayName);

    /** Al borrar la cuenta: se anonimizan (no se borran) para no romper hilos de otros usuarios. */
    @Modifying
    @Query("""
            update CommunityComment c
               set c.authorUserId = null,
                   c.authorDisplayName = :deletedName,
                   c.authorAvatarUrl = null,
                   c.authorVerified = false
             where c.authorUserId = :userId
            """)
    void anonymizeAuthor(UUID userId, String deletedName);
}
