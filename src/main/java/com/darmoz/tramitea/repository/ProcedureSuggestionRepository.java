package com.darmoz.tramitea.repository;

import com.darmoz.tramitea.entity.ProcedureSuggestion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ProcedureSuggestionRepository extends JpaRepository<ProcedureSuggestion, UUID> {
}
