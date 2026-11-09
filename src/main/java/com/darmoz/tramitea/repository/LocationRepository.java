package com.darmoz.tramitea.repository;

import com.darmoz.tramitea.entity.Location;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface LocationRepository extends JpaRepository<Location, UUID> {

    /** Activas y, si son provincias, con el país también activo. */
    @Query("""
            select l from Location l
            where l.active = true
              and (l.parentId is null
                   or l.parentId in (select c.id from Location c where c.active = true))
            order by l.name""")
    List<Location> findVisible();

    List<Location> findByParentIdIn(Collection<UUID> parentIds);
}
