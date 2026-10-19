package com.darmoz.tramitea.service;

import com.darmoz.tramitea.dto.response.SavedTaskResponse;
import com.darmoz.tramitea.entity.SavedTask;
import com.darmoz.tramitea.entity.SavedTaskStatus;
import com.darmoz.tramitea.exception.NotFoundException;
import com.darmoz.tramitea.repository.SavedTaskRepository;
import com.darmoz.tramitea.security.AuthenticatedUser;
import com.darmoz.tramitea.util.TextNormalizer;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/** Toda operacion se acota al usuario del token: una tarea ajena se trata como inexistente. */
@Service
public class SavedTaskService {

    private final SavedTaskRepository savedTaskRepository;
    private final ProcedureService procedureService;
    private final CitizenUserService citizenUserService;

    public SavedTaskService(SavedTaskRepository savedTaskRepository, ProcedureService procedureService,
                            CitizenUserService citizenUserService) {
        this.savedTaskRepository = savedTaskRepository;
        this.procedureService = procedureService;
        this.citizenUserService = citizenUserService;
    }

    @Transactional(readOnly = true)
    public List<SavedTaskResponse> list(AuthenticatedUser user) {
        return savedTaskRepository.findByUserIdOrderByCreatedAtDesc(user.id()).stream()
                .map(SavedTaskResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public boolean isSaved(UUID procedureId, AuthenticatedUser user) {
        return savedTaskRepository.existsByUserIdAndProcedureId(user.id(), procedureId);
    }

    /** Idempotente: si ya estaba guardado devuelve la tarea existente. */
    @Transactional
    public SavedTaskResponse save(UUID procedureId, AuthenticatedUser user) {
        procedureService.requireExists(procedureId);
        citizenUserService.getCurrent(user);
        SavedTask task = savedTaskRepository.findByUserIdAndProcedureId(user.id(), procedureId)
                .orElseGet(() -> savedTaskRepository.save(new SavedTask(user.id(), procedureId)));
        return SavedTaskResponse.from(task);
    }

    @Transactional
    public SavedTaskResponse updateStatus(UUID taskId, SavedTaskStatus status, AuthenticatedUser user) {
        SavedTask task = findOwned(taskId, user);
        task.changeStatus(status);
        return SavedTaskResponse.from(task);
    }

    /** La nota se recorta; si queda vacia se borra. */
    @Transactional
    public SavedTaskResponse updateNotes(UUID taskId, String notes, AuthenticatedUser user) {
        SavedTask task = findOwned(taskId, user);
        task.changeNotes(TextNormalizer.trimToNull(notes));
        return SavedTaskResponse.from(task);
    }

    /** Idempotente, igual que el mock: borrar una tarea inexistente no falla. */
    @Transactional
    public void remove(UUID taskId, AuthenticatedUser user) {
        savedTaskRepository.findByIdAndUserId(taskId, user.id()).ifPresent(savedTaskRepository::delete);
    }

    private SavedTask findOwned(UUID taskId, AuthenticatedUser user) {
        return savedTaskRepository.findByIdAndUserId(taskId, user.id())
                .orElseThrow(() -> new NotFoundException("Tarea no encontrada"));
    }
}
