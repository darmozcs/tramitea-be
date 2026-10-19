package com.darmoz.tramitea.service;

import com.darmoz.tramitea.dto.NotificationPreferencesDto;
import com.darmoz.tramitea.dto.request.Requests;
import com.darmoz.tramitea.dto.response.CitizenProfileResponse;
import com.darmoz.tramitea.entity.CitizenUser;
import com.darmoz.tramitea.entity.CommunityComment;
import com.darmoz.tramitea.exception.ValidationException;
import com.darmoz.tramitea.repository.CitizenUserRepository;
import com.darmoz.tramitea.repository.CommunityCommentRepository;
import com.darmoz.tramitea.repository.SavedTaskRepository;
import com.darmoz.tramitea.repository.VoteRepository;
import com.darmoz.tramitea.security.AuthenticatedUser;
import com.darmoz.tramitea.util.TextNormalizer;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProfileService {

    private final CitizenUserService citizenUserService;
    private final CitizenUserRepository citizenUserRepository;
    private final CommunityCommentRepository commentRepository;
    private final SavedTaskRepository savedTaskRepository;
    private final VoteRepository voteRepository;

    public ProfileService(CitizenUserService citizenUserService, CitizenUserRepository citizenUserRepository,
                          CommunityCommentRepository commentRepository, SavedTaskRepository savedTaskRepository,
                          VoteRepository voteRepository) {
        this.citizenUserService = citizenUserService;
        this.citizenUserRepository = citizenUserRepository;
        this.commentRepository = commentRepository;
        this.savedTaskRepository = savedTaskRepository;
        this.voteRepository = voteRepository;
    }

    @Transactional
    public CitizenProfileResponse get(AuthenticatedUser user) {
        return CitizenProfileResponse.from(citizenUserService.getCurrent(user));
    }

    /**
     * El email tambien es el de login: si cambia, tiene que seguir siendo
     * unico. El front pide un /auth/refresh despues para que la cookie lleve
     * el nombre y el email nuevos.
     */
    @Transactional
    public CitizenProfileResponse update(Requests.UpdateProfile payload, AuthenticatedUser user) {
        String firstName = TextNormalizer.trimToNull(payload.firstName());
        String lastName = TextNormalizer.trimToNull(payload.lastName());
        String email = AuthService.normalizeEmail(payload.email());
        String phone = TextNormalizer.trimToNull(payload.phone());
        String locality = TextNormalizer.trimToNull(payload.locality());

        boolean valid = firstName != null && firstName.length() <= 100
                && lastName != null && lastName.length() <= 100
                && AuthService.isValidEmail(email)
                && (phone == null || phone.length() <= 50)
                && (locality == null || locality.length() <= 100);
        if (!valid) {
            throw new ValidationException(ValidationException.INVALID_PROFILE_PAYLOAD, "Perfil invalido");
        }

        CitizenUser citizen = citizenUserService.getCurrent(user);
        if (!email.equals(citizen.getEmail()) && citizenUserRepository.existsByEmail(email)) {
            throw AuthService.emailTaken();
        }
        citizen.updatePersonalData(firstName, lastName, email, phone, payload.department(), locality);
        commentRepository.renameAuthor(citizen.getId(), citizen.getDisplayName());
        return CitizenProfileResponse.from(citizen);
    }

    @Transactional
    public NotificationPreferencesDto updateNotificationPreferences(NotificationPreferencesDto preferences,
                                                                    AuthenticatedUser user) {
        CitizenUser citizen = citizenUserService.getCurrent(user);
        citizen.updateNotificationPreferences(preferences.documentExpiry(), preferences.appointmentReminders(),
                preferences.newsAndUpdates());
        return NotificationPreferencesDto.from(citizen);
    }

    /**
     * Borra la cuenta: tareas, votos y la fila de usuario (con eso el login
     * deja de existir). Los comentarios se anonimizan. Los tokens ya
     * emitidos pasan a responder 401 (ver CitizenUserService.getCurrent).
     */
    @Transactional
    public void deleteAccount(AuthenticatedUser user) {
        savedTaskRepository.deleteByUserId(user.id());
        voteRepository.deleteByUserId(user.id());
        commentRepository.anonymizeAuthor(user.id(), CommunityComment.DELETED_AUTHOR_NAME);
        citizenUserRepository.deleteById(user.id());
    }
}
