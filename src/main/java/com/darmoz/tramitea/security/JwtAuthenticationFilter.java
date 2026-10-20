package com.darmoz.tramitea.security;

import com.darmoz.tramitea.exception.UnauthorizedException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Autentica requests a partir del Bearer (Authorization: Bearer ...): el token
 * propio o el de darmoz-auth, según auth.mode (ver {@link TokenVerifier}). El front lo
 * guarda en una cookie httpOnly y lo reenvia desde sus route handlers. No
 * rechaza requests con token ausente/invalido: solo deja el SecurityContext
 * vacio y es authorizeHttpRequests() el que decide si la ruta requiere
 * autenticacion (el catalogo es publico).
 */
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final TokenVerifier tokenVerifier;

    public JwtAuthenticationFilter(TokenVerifier tokenVerifier) {
        this.tokenVerifier = tokenVerifier;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String token = extractBearerToken(request.getHeader(HttpHeaders.AUTHORIZATION));

        if (token != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            try {
                AuthenticatedUser principal = tokenVerifier.verify(token);
                var authentication = new UsernamePasswordAuthenticationToken(principal, null, List.of());
                SecurityContextHolder.getContext().setAuthentication(authentication);
            } catch (JwtException | IllegalArgumentException | UnauthorizedException ignored) {
                // token invalido/expirado/malformado (o sub que no es UUID): se sigue sin autenticacion
            }
        }

        filterChain.doFilter(request, response);
    }

    private String extractBearerToken(String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.startsWith(BEARER_PREFIX)) {
            return null;
        }
        return authorizationHeader.substring(BEARER_PREFIX.length());
    }
}
