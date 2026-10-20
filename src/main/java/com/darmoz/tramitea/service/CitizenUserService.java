package com.darmoz.tramitea.service;

import com.darmoz.tramitea.entity.CitizenUser;
import com.darmoz.tramitea.exception.UnauthorizedException;
import com.darmoz.tramitea.repository.CitizenUserRepository;
import com.darmoz.tramitea.security.AuthenticatedUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
public class CitizenUserService {

    private final CitizenUserRepository citizenUserRepository;

    public CitizenUserService(CitizenUserRepository citizenUserRepository) {
        this.citizenUserRepository = citizenUserRepository;
    }

    /**
     * Usuario del token. Si la cuenta se borro mientras el token seguia
     * vigente, responde 401 en vez de recrearla.
     */
    @Transactional(readOnly = true)
    public CitizenUser getCurrent(AuthenticatedUser user) {
        return citizenUserRepository.findById(user.id())
                .orElseThrow(UnauthorizedException::new);
    }

    /**
     * Modo darmoz: perfil de Tramitea para el usuario de darmoz-auth
     * {@code idAuthUser} (sub del token), creándolo la primera vez.
     * <ol>
     *   <li>Ya vinculado por id_auth_user: se usa (y se sincroniza el email).</li>
     *   <li>Existe un perfil con el mismo email (cuenta local previa o una cuenta de
     *   darmoz-auth recreada): se vincula, conservando sus tareas y votos.</li>
     *   <li>Si no, se crea uno nuevo sin password ni nombres.</li>
     * </ol>
     * Dos requests simultáneos del mismo usuario nuevo pueden chocar en el UNIQUE;
     * el llamador reintenta (ver DarmozTokenVerifier).
     */
    @Transactional
    public CitizenUser resolveByAuthUser(UUID idAuthUser, String email) {
        String normalizedEmail = AuthService.normalizeEmail(email);

        Optional<CitizenUser> linked = citizenUserRepository.findByIdAuthUser(idAuthUser);
        if (linked.isPresent()) {
            CitizenUser user = linked.get();
            if (normalizedEmail != null && !normalizedEmail.equals(user.getEmail())
                    && !citizenUserRepository.existsByEmail(normalizedEmail)) {
                user.syncEmail(normalizedEmail);
            }
            return user;
        }

        if (normalizedEmail == null) {
            throw new UnauthorizedException();
        }

        Optional<CitizenUser> sameEmail = citizenUserRepository.findByEmail(normalizedEmail);
        if (sameEmail.isPresent()) {
            CitizenUser user = sameEmail.get();
            user.linkAuthUser(idAuthUser);
            return user;
        }

        return citizenUserRepository.saveAndFlush(CitizenUser.fromAuthUser(idAuthUser, normalizedEmail));
    }
}
