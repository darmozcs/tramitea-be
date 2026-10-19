package com.darmoz.tramitea.dto.response;

import com.darmoz.tramitea.dto.NotificationPreferencesDto;
import com.darmoz.tramitea.entity.CitizenUser;
import com.darmoz.tramitea.entity.UruguayDepartment;

import java.time.Instant;
import java.util.UUID;

public record CitizenProfileResponse(UUID id, String firstName, String lastName, String email, String nationalId,
                                     boolean nationalIdVerified, String phone, UruguayDepartment department,
                                     String locality, Instant memberSince,
                                     NotificationPreferencesDto notificationPreferences, Instant updatedAt) {

    public static CitizenProfileResponse from(CitizenUser u) {
        return new CitizenProfileResponse(u.getId(), u.getFirstName(), u.getLastName(), u.getEmail(),
                u.getNationalId(), u.isNationalIdVerified(), u.getPhone(), u.getDepartment(), u.getLocality(),
                u.getCreatedAt(), NotificationPreferencesDto.from(u), u.getUpdatedAt());
    }
}
