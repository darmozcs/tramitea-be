package com.darmoz.tramitea.service;

import com.darmoz.tramitea.dto.request.Requests;
import com.darmoz.tramitea.dto.response.SuggestionResponse;
import com.darmoz.tramitea.entity.ProcedureSuggestion;
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

    public SuggestionService(ProcedureSuggestionRepository suggestionRepository,
                             InstitutionRepository institutionRepository) {
        this.suggestionRepository = suggestionRepository;
        this.institutionRepository = institutionRepository;
    }

    /** @param user null si la sugerencia es anonima. */
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

        UUID userId = user == null ? null : user.id();
        ProcedureSuggestion suggestion = suggestionRepository.save(
                new ProcedureSuggestion(title, payload.institutionId(), details, contactEmail, userId));
        return SuggestionResponse.from(suggestion);
    }
}
