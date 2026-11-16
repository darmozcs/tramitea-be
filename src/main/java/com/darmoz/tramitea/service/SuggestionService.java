package com.darmoz.tramitea.service;

import com.darmoz.tramitea.dto.request.Requests;
import com.darmoz.tramitea.dto.response.SuggestionResponse;
import com.darmoz.tramitea.entity.CitizenUser;
import com.darmoz.tramitea.entity.Location;
import com.darmoz.tramitea.entity.ProcedureSuggestion;
import com.darmoz.tramitea.exception.ForbiddenException;
import com.darmoz.tramitea.exception.ValidationException;
import com.darmoz.tramitea.repository.InstitutionRepository;
import com.darmoz.tramitea.repository.ProcedureSuggestionRepository;
import com.darmoz.tramitea.security.AuthenticatedUser;
import com.darmoz.tramitea.util.TextNormalizer;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class SuggestionService {

    private final ProcedureSuggestionRepository suggestionRepository;
    private final InstitutionRepository institutionRepository;
    private final CitizenUserService citizenUserService;
    private final LocationService locationService;

    public SuggestionService(ProcedureSuggestionRepository suggestionRepository,
                             InstitutionRepository institutionRepository, CitizenUserService citizenUserService,
                             LocationService locationService) {
        this.suggestionRepository = suggestionRepository;
        this.institutionRepository = institutionRepository;
        this.citizenUserService = citizenUserService;
        this.locationService = locationService;
    }

    /**
     * Solo usuarios logueados y con ubicacion. La sugerencia va a su
     * ubicacion o, si es una provincia, a su pais (tramite nacional); nunca a
     * otra. Sin `locationId` en el payload se usa la del usuario.
     */
    @Transactional
    public SuggestionResponse suggest(Requests.SuggestProcedure payload, AuthenticatedUser user) {
        String title = TextNormalizer.trimToNull(payload.title());
        String details = TextNormalizer.trimToNull(payload.details());
        String contactEmail = TextNormalizer.trimToNull(payload.contactEmail());

        boolean valid = title != null && title.length() <= 255
                && details != null && details.length() <= 5000
                && (contactEmail == null || (contactEmail.contains("@") && contactEmail.length() <= 255))
                && payload.institutionId() != null && institutionRepository.existsById(payload.institutionId());
        if (!valid) {
            throw new ValidationException(ValidationException.INVALID_SUGGESTION_PAYLOAD, "Sugerencia invalida");
        }

        CitizenUser citizen = citizenUserService.getCurrent(user);
        Location home = locationService.findVisible(citizen.getLocationId());
        if (home == null) {
            throw new ValidationException(ValidationException.LOCATION_REQUIRED,
                    "Elegi tu ubicacion en el perfil para sugerir tramites");
        }

        UUID target = payload.locationId() == null ? home.getId() : payload.locationId();
        boolean allowed = (target.equals(home.getId()) || target.equals(home.getParentId()))
                && locationService.findVisible(target) != null;
        if (!allowed) {
            throw new ForbiddenException(ForbiddenException.LOCATION_NOT_ALLOWED,
                    "Solo podes sugerir tramites de tu provincia o de tu pais");
        }

        ProcedureSuggestion suggestion = suggestionRepository.save(
                new ProcedureSuggestion(title, payload.institutionId(), details, contactEmail, citizen.getId(),
                        target));
        return SuggestionResponse.from(suggestion);
    }
}
