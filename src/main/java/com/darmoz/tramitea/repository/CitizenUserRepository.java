package com.darmoz.tramitea.repository;

import com.darmoz.tramitea.entity.CitizenUser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/** Los emails se guardan en minusculas: buscar siempre con el valor ya normalizado. */
public interface CitizenUserRepository extends JpaRepository<CitizenUser, UUID> {

    Optional<CitizenUser> findByEmail(String email);

    Optional<CitizenUser> findByIdAuthUser(UUID idAuthUser);

    boolean existsByEmail(String email);
}
