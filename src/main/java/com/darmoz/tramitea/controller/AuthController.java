package com.darmoz.tramitea.controller;

import com.darmoz.tramitea.config.AuthMode;
import com.darmoz.tramitea.dto.request.Requests;
import com.darmoz.tramitea.dto.response.AuthResponse;
import com.darmoz.tramitea.security.AuthenticatedUser;
import com.darmoz.tramitea.service.AuthService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Solo lo consumen los route handlers /api/auth/* del front, que aplican el
 * rate limiting por IP y guardan el token en la cookie httpOnly.
 */
@RestController
@ConditionalOnProperty(name = AuthMode.PROPERTY, havingValue = AuthMode.LOCAL, matchIfMissing = true)
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthResponse register(@RequestBody Requests.Register payload) {
        return authService.register(payload);
    }

    @PostMapping("/sign-in")
    public AuthResponse signIn(@RequestBody Requests.SignIn payload) {
        return authService.signIn(payload);
    }

    @PostMapping("/refresh")
    public AuthResponse refresh(@AuthenticationPrincipal AuthenticatedUser user) {
        return authService.refresh(user);
    }
}
