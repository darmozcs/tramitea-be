package com.darmoz.tramitea.service;

import com.darmoz.tramitea.dto.response.CommunityCommentResponse;
import com.darmoz.tramitea.entity.CitizenUser;
import com.darmoz.tramitea.entity.CommunityComment;
import com.darmoz.tramitea.entity.Vote;
import com.darmoz.tramitea.entity.VoteTargetType;
import com.darmoz.tramitea.entity.VoteValue;
import com.darmoz.tramitea.exception.ValidationException;
import com.darmoz.tramitea.repository.CommunityCommentRepository;
import com.darmoz.tramitea.repository.VoteRepository;
import com.darmoz.tramitea.security.AuthenticatedUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class CommunityService {

    private final CommunityCommentRepository commentRepository;
    private final VoteRepository voteRepository;
    private final ProcedureService procedureService;
    private final CitizenUserService citizenUserService;

    public CommunityService(CommunityCommentRepository commentRepository, VoteRepository voteRepository,
                            ProcedureService procedureService, CitizenUserService citizenUserService) {
        this.commentRepository = commentRepository;
        this.voteRepository = voteRepository;
        this.procedureService = procedureService;
        this.citizenUserService = citizenUserService;
    }

    @Transactional(readOnly = true)
    public List<CommunityCommentResponse> getComments(UUID procedureId) {
        procedureService.requireExists(procedureId);
        return commentRepository.findByProcedureIdOrderByCreatedAtAsc(procedureId).stream()
                .map(CommunityCommentResponse::from)
                .toList();
    }

    @Transactional
    public CommunityCommentResponse postComment(UUID procedureId, String content, UUID parentId,
                                                AuthenticatedUser user) {
        procedureService.requireExists(procedureId);
        if (parentId != null && !commentRepository.existsByIdAndProcedureId(parentId, procedureId)) {
            throw new ValidationException(ValidationException.INVALID_COMMENT_PAYLOAD,
                    "El comentario padre no existe en este tramite");
        }
        CitizenUser author = citizenUserService.getCurrent(user);
        CommunityComment comment = commentRepository.save(
                new CommunityComment(procedureId, parentId, author, content.trim()));
        return CommunityCommentResponse.from(comment);
    }

    @Transactional(readOnly = true)
    public long getGuideUsefulness(UUID procedureId) {
        procedureService.requireExists(procedureId);
        return countUseful(procedureId);
    }

    /** Un voto por usuario: votar otra vez reemplaza el valor anterior. */
    @Transactional
    public long submitGuideVote(UUID procedureId, VoteValue value, AuthenticatedUser user) {
        procedureService.requireExists(procedureId);
        citizenUserService.getCurrent(user);
        voteRepository.findByUserIdAndTargetTypeAndTargetId(user.id(), VoteTargetType.PROCEDURE_GUIDE, procedureId)
                .ifPresentOrElse(
                        vote -> vote.changeValue(value),
                        () -> voteRepository.save(
                                new Vote(user.id(), procedureId, VoteTargetType.PROCEDURE_GUIDE, procedureId, value)));
        voteRepository.flush();
        return countUseful(procedureId);
    }

    private long countUseful(UUID procedureId) {
        return voteRepository.countByTargetTypeAndTargetIdAndValue(
                VoteTargetType.PROCEDURE_GUIDE, procedureId, VoteValue.USEFUL);
    }
}
