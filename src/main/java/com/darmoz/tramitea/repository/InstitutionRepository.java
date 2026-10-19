package com.darmoz.tramitea.repository;

import com.darmoz.tramitea.entity.Institution;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface InstitutionRepository extends JpaRepository<Institution, UUID> {
}
