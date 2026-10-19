package com.darmoz.tramitea.security;

import java.util.UUID;

/** Principal de Spring Security: el usuario de darmoz-auth que firmo el request. */
public record AuthenticatedUser(UUID id, String email) {
}
