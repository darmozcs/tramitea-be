package com.darmoz.tramitea.service;

import com.darmoz.tramitea.entity.CitizenUser;
import com.darmoz.tramitea.exception.UnauthorizedException;
import com.darmoz.tramitea.repository.CitizenUserRepository;
import com.darmoz.tramitea.security.AuthenticatedUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
}
