package com.darmoz.tramitea.config;

/**
 * Modo de autenticación (property `auth.mode`, env AUTH_MODE).
 * <ul>
 *   <li>{@code local} (default): cuentas propias en tr_users y JWT HS256 que
 *   emite este servicio. Es el modo de producción (Railway).</li>
 *   <li>{@code darmoz}: las cuentas viven en darmoz-auth; este servicio solo
 *   verifica sus tokens RS256 y relaciona cada usuario por
 *   tr_users.id_auth_user. Es el modo de testing (server de Darmoz).</li>
 * </ul>
 */
public final class AuthMode {

    public static final String PROPERTY = "auth.mode";
    public static final String LOCAL = "local";
    public static final String DARMOZ = "darmoz";

    private AuthMode() {
    }
}
