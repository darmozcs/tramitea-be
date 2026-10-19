package com.darmoz.tramitea.repository;

import com.darmoz.tramitea.entity.Procedure;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProcedureRepository extends JpaRepository<Procedure, UUID>, JpaSpecificationExecutor<Procedure> {

    @Query("select p from Procedure p where p.slugEs = :slug or p.slugEn = :slug")
    Optional<Procedure> findBySlug(String slug);

    @Query("select p from Procedure p order by p.viewCount desc, p.displayOrder asc")
    List<Procedure> findPopular(Pageable pageable);

    /** Incremento atomico: no pisa visitas concurrentes como haria un read-modify-write. */
    @Modifying
    @Query("update Procedure p set p.viewCount = p.viewCount + 1 where p.id = :id")
    void incrementViewCount(UUID id);
}
