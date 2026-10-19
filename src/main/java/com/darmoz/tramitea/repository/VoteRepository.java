package com.darmoz.tramitea.repository;

import com.darmoz.tramitea.entity.Vote;
import com.darmoz.tramitea.entity.VoteTargetType;
import com.darmoz.tramitea.entity.VoteValue;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;
import java.util.UUID;

public interface VoteRepository extends JpaRepository<Vote, UUID> {

    long countByTargetTypeAndTargetIdAndValue(VoteTargetType targetType, UUID targetId, VoteValue value);

    Optional<Vote> findByUserIdAndTargetTypeAndTargetId(UUID userId, VoteTargetType targetType, UUID targetId);

    @Modifying
    @Query("delete from Vote v where v.userId = :userId")
    void deleteByUserId(UUID userId);
}
