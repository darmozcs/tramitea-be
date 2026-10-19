package com.darmoz.tramitea.dto;

import com.darmoz.tramitea.entity.CitizenUser;
import jakarta.validation.constraints.NotNull;

/** Request y response de las preferencias (el front manda siempre el objeto completo). */
public record NotificationPreferencesDto(@NotNull Boolean documentExpiry,
                                         @NotNull Boolean appointmentReminders,
                                         @NotNull Boolean newsAndUpdates) {

    public static NotificationPreferencesDto from(CitizenUser u) {
        return new NotificationPreferencesDto(u.isNotifyDocumentExpiry(), u.isNotifyAppointmentReminders(),
                u.isNotifyNewsAndUpdates());
    }
}
