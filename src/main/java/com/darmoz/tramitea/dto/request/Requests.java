package com.darmoz.tramitea.dto.request;

import com.darmoz.tramitea.entity.SavedTaskStatus;
import com.darmoz.tramitea.entity.UruguayDepartment;
import com.darmoz.tramitea.entity.VoteValue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

/** Bodies de request de la API, agrupados por ser records de una linea. */
public final class Requests {

    private Requests() {
    }

    /** Validado a mano en AuthService para responder `invalid-payload`, igual que el route handler del front. */
    public record Register(String firstName, String lastName, String email, String password) {
    }

    public record SignIn(String email, String password) {
    }

    public record CreateComment(@NotBlank @Size(max = 2000) String content, UUID parentId) {
    }

    public record SubmitVote(@NotNull VoteValue value) {
    }

    /** Validado a mano en el servicio para responder el codigo `invalid-suggestion-payload` del front. */
    public record SuggestProcedure(String title, UUID institutionId, String details, String contactEmail) {
    }

    public record SaveTask(@NotNull UUID procedureId) {
    }

    public record UpdateTaskStatus(@NotNull SavedTaskStatus status) {
    }

    public record UpdateTaskNotes(@NotNull @Size(max = 2000) String notes) {
    }

    /** Validado a mano en el servicio para responder el codigo `invalid-profile-payload` del front. */
    public record UpdateProfile(String firstName, String lastName, String email, String phone,
                                UruguayDepartment department, String locality) {
    }
}
