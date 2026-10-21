package com.darmoz.tramitea.controller;

import com.darmoz.tramitea.dto.request.Requests;
import com.darmoz.tramitea.dto.response.SavedTaskResponse;
import com.darmoz.tramitea.security.AuthenticatedUser;
import com.darmoz.tramitea.service.SavedTaskService;
import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@Tag(name = "Tareas guardadas", description = "Trámites que el usuario guardó para hacer.")
@RestController
@RequestMapping("/user/saved-tasks")
public class SavedTaskController {

    private final SavedTaskService savedTaskService;

    public SavedTaskController(SavedTaskService savedTaskService) {
        this.savedTaskService = savedTaskService;
    }

    @GetMapping
    @Operation(summary = "Mis tareas")
    public List<SavedTaskResponse> list(@AuthenticationPrincipal AuthenticatedUser user) {
        return savedTaskService.list(user);
    }

    @GetMapping("/{procedureId}/is-saved")
    @Operation(summary = "¿Está guardado?", description = "Recibe el id del trámite, no el de la tarea.")
    public boolean isSaved(@PathVariable UUID procedureId, @AuthenticationPrincipal AuthenticatedUser user) {
        return savedTaskService.isSaved(procedureId, user);
    }

    @PostMapping
    @Operation(summary = "Guardar un trámite", description = "Queda en `PENDING`. Si ya estaba guardado devuelve la tarea existente.")
    public SavedTaskResponse save(@Valid @RequestBody Requests.SaveTask payload,
                                  @AuthenticationPrincipal AuthenticatedUser user) {
        return savedTaskService.save(payload.procedureId(), user);
    }

    @PatchMapping("/{taskId}")
    @Operation(summary = "Cambiar el estado", description = "404 si la tarea no existe o es de otro usuario.")
    public SavedTaskResponse updateStatus(@PathVariable UUID taskId,
                                          @Valid @RequestBody Requests.UpdateTaskStatus payload,
                                          @AuthenticationPrincipal AuthenticatedUser user) {
        return savedTaskService.updateStatus(taskId, payload.status(), user);
    }

    @PatchMapping("/{taskId}/notes")
    @Operation(summary = "Editar las notas", description = "Hasta 2000 caracteres; puede ser vacío.")
    public SavedTaskResponse updateNotes(@PathVariable UUID taskId,
                                         @Valid @RequestBody Requests.UpdateTaskNotes payload,
                                         @AuthenticationPrincipal AuthenticatedUser user) {
        return savedTaskService.updateNotes(taskId, payload.notes(), user);
    }

    @DeleteMapping("/{taskId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Quitar de guardados")
    public void remove(@PathVariable UUID taskId, @AuthenticationPrincipal AuthenticatedUser user) {
        savedTaskService.remove(taskId, user);
    }
}
