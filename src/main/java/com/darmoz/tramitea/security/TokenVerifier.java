package com.darmoz.tramitea.security;

/**
 * Verifica el Bearer de cada request y devuelve el usuario de Tramitea al que
 * corresponde. Hay una implementación por modo de auth (`auth.mode`):
 * {@link SessionTokenService} (local, JWT HS256 propio) y
 * {@link DarmozTokenVerifier} (darmoz-auth, JWT RS256).
 *
 * @throws io.jsonwebtoken.JwtException si el token no es válido
 */
public interface TokenVerifier {

    AuthenticatedUser verify(String token);
}
