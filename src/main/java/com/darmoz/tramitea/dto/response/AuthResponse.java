package com.darmoz.tramitea.dto.response;

import com.darmoz.tramitea.entity.CitizenUser;

import java.util.UUID;

/** `user` es el `SessionUser` del front; `token` va a la cookie httpOnly que setea el front. */
public record AuthResponse(SessionUser user, String token, long expiresIn) {

    public static AuthResponse of(CitizenUser user, String token, long expiresIn) {
        return new AuthResponse(
                new SessionUser(user.getId(), user.getFirstName(), user.getLastName(), user.getEmail()),
                token, expiresIn);
    }

    public record SessionUser(UUID id, String firstName, String lastName, String email) {
    }
}
