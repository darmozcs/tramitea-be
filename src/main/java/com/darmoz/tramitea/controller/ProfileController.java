package com.darmoz.tramitea.controller;

import com.darmoz.tramitea.dto.NotificationPreferencesDto;
import com.darmoz.tramitea.dto.request.Requests;
import com.darmoz.tramitea.dto.response.CitizenProfileResponse;
import com.darmoz.tramitea.security.AuthenticatedUser;
import com.darmoz.tramitea.service.ProfileService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/user/profile")
public class ProfileController {

    private final ProfileService profileService;

    public ProfileController(ProfileService profileService) {
        this.profileService = profileService;
    }

    @GetMapping
    public CitizenProfileResponse get(@AuthenticationPrincipal AuthenticatedUser user) {
        return profileService.get(user);
    }

    @PatchMapping
    public CitizenProfileResponse update(@RequestBody Requests.UpdateProfile payload,
                                         @AuthenticationPrincipal AuthenticatedUser user) {
        return profileService.update(payload, user);
    }

    @PatchMapping("/notification-preferences")
    public NotificationPreferencesDto updateNotificationPreferences(
            @Valid @RequestBody NotificationPreferencesDto payload,
            @AuthenticationPrincipal AuthenticatedUser user) {
        return profileService.updateNotificationPreferences(payload, user);
    }

    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteAccount(@AuthenticationPrincipal AuthenticatedUser user) {
        profileService.deleteAccount(user);
    }
}
