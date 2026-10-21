package com.darmoz.tramitea.controller;

import com.darmoz.tramitea.config.AuthMode;
import com.darmoz.tramitea.dto.request.Requests;
import com.darmoz.tramitea.dto.response.AuthResponse;
import com.darmoz.tramitea.security.AuthenticatedUser;
import com.darmoz.tramitea.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Auth local", description = "Solo existen con AUTH_MODE=local (producción). Con AUTH_MODE=darmoz (testing) el login es contra darmoz-auth.")
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
    @Operation(summary = "Crear cuenta", description = "Nombres hasta 100, password de 8 a 72 caracteres. 409 `email-already-registered` si el email ya existe.")
    public AuthResponse register(@RequestBody Requests.Register payload) {
        return authService.register(payload);
    }

    @PostMapping("/sign-in")
    @Operation(summary = "Iniciar sesión", description = "Devuelve el token (HS256, 30 minutos). 401 `invalid-credentials` si no coinciden.")
    public AuthResponse signIn(@RequestBody Requests.SignIn payload) {
        return authService.signIn(payload);
    }

    @PostMapping("/refresh")
    @Operation(summary = "Renovar el token", description = "Con un token vigente devuelve uno nuevo con los datos actuales del usuario.")
    public AuthResponse refresh(@AuthenticationPrincipal AuthenticatedUser user) {
        return authService.refresh(user);
    }
}
