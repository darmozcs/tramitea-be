package com.darmoz.tramitea.repository;

import com.darmoz.tramitea.entity.SavedTask;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SavedTaskRepository extends JpaRepository<SavedTask, UUID> {

    List<SavedTask> findByUserIdOrderByCreatedAtDesc(UUID userId);

    Optional<SavedTask> findByIdAndUserId(UUID id, UUID userId);

    Optional<SavedTask> findByUserIdAndProcedureId(UUID userId, UUID procedureId);

    boolean existsByUserIdAndProcedureId(UUID userId, UUID procedureId);

    @Modifying
    @Query("delete from SavedTask t where t.userId = :userId")
    void deleteByUserId(UUID userId);
}
