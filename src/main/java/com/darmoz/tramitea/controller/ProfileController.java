package com.darmoz.tramitea.controller;

import com.darmoz.tramitea.dto.NotificationPreferencesDto;
import com.darmoz.tramitea.dto.request.Requests;
import com.darmoz.tramitea.dto.response.CitizenProfileResponse;
import com.darmoz.tramitea.security.AuthenticatedUser;
import com.darmoz.tramitea.service.ProfileService;
import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Perfil", description = "Datos del usuario del token.")
@RestController
@RequestMapping("/user/profile")
public class ProfileController {

    private final ProfileService profileService;

    public ProfileController(ProfileService profileService) {
        this.profileService = profileService;
    }

    @GetMapping
    @Operation(summary = "Ver mi perfil", description = "La primera llamada con un token de darmoz-auth crea el perfil.")
    public CitizenProfileResponse get(@AuthenticationPrincipal AuthenticatedUser user) {
        return profileService.get(user);
    }

    @PatchMapping
    @Operation(summary = "Editar mi perfil", description = "`firstName` y `lastName` obligatorios (hasta 100). Con AUTH_MODE=darmoz el `email` se ignora: manda el de darmoz-auth. `department` es el código de dos letras (`MO`).")
    public CitizenProfileResponse update(@RequestBody Requests.UpdateProfile payload,
                                         @AuthenticationPrincipal AuthenticatedUser user) {
        return profileService.update(payload, user);
    }

    @PatchMapping("/notification-preferences")
    @Operation(summary = "Preferencias de notificación", description = "Hay que mandar los tres campos.")
    public NotificationPreferencesDto updateNotificationPreferences(
            @Valid @RequestBody NotificationPreferencesDto payload,
            @AuthenticationPrincipal AuthenticatedUser user) {
        return profileService.updateNotificationPreferences(payload, user);
    }

    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Borrar mi cuenta", description = "Borra el perfil, las tareas y los votos; los comentarios quedan anónimos. No deshabilita la cuenta en darmoz-auth.")
    public void deleteAccount(@AuthenticationPrincipal AuthenticatedUser user) {
        profileService.deleteAccount(user);
    }
}
