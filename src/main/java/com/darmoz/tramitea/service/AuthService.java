package com.darmoz.tramitea.service;

import com.darmoz.tramitea.config.AuthMode;
import com.darmoz.tramitea.dto.request.Requests;
import com.darmoz.tramitea.dto.response.AuthResponse;
import com.darmoz.tramitea.entity.CitizenUser;
import com.darmoz.tramitea.exception.ConflictException;
import com.darmoz.tramitea.exception.UnauthorizedException;
import com.darmoz.tramitea.exception.ValidationException;
import com.darmoz.tramitea.repository.CitizenUserRepository;
import com.darmoz.tramitea.security.AuthenticatedUser;
import com.darmoz.tramitea.security.SessionTokenService;
import com.darmoz.tramitea.util.TextNormalizer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Registro, login y refresh. La sesion es stateless (JWT de 30 min que el
 * front renueva con /auth/refresh); el logout es borrar la cookie en el front.
 */
@Service
@ConditionalOnProperty(name = AuthMode.PROPERTY, havingValue = AuthMode.LOCAL, matchIfMissing = true)
public class AuthService {

    public static final int PASSWORD_MIN_LENGTH = 8;
    /** BCrypt ignora lo que pase de 72 bytes. */
    public static final int PASSWORD_MAX_LENGTH = 72;

    private static final Pattern EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    private final CitizenUserRepository citizenUserRepository;
    private final CitizenUserService citizenUserService;
    private final PasswordEncoder passwordEncoder;
    private final SessionTokenService sessionTokenService;

    public AuthService(CitizenUserRepository citizenUserRepository, CitizenUserService citizenUserService,
                       PasswordEncoder passwordEncoder, SessionTokenService sessionTokenService) {
        this.citizenUserRepository = citizenUserRepository;
        this.citizenUserService = citizenUserService;
        this.passwordEncoder = passwordEncoder;
        this.sessionTokenService = sessionTokenService;
    }

    @Transactional
    public AuthResponse register(Requests.Register payload) {
        String firstName = TextNormalizer.trimToNull(payload.firstName());
        String lastName = TextNormalizer.trimToNull(payload.lastName());
        String email = normalizeEmail(payload.email());
        String password = payload.password();

        boolean valid = firstName != null && firstName.length() <= 100
                && lastName != null && lastName.length() <= 100
                && isValidEmail(email)
                && password != null && password.length() >= PASSWORD_MIN_LENGTH
                && password.length() <= PASSWORD_MAX_LENGTH;
        if (!valid) {
            throw new ValidationException(ValidationException.INVALID_PAYLOAD, "Datos de registro invalidos");
        }
        if (citizenUserRepository.existsByEmail(email)) {
            throw emailTaken();
        }

        CitizenUser user;
        try {
            user = citizenUserRepository.saveAndFlush(
                    new CitizenUser(email, passwordEncoder.encode(password), firstName, lastName));
        } catch (DataIntegrityViolationException e) {
            // Dos registros simultaneos con el mismo email: gana el primero.
            throw emailTaken();
        }
        return issue(user);
    }

    /** Mismo 401 para email inexistente y password incorrecta: no revela que cuentas existen. */
    @Transactional(readOnly = true)
    public AuthResponse signIn(Requests.SignIn payload) {
        String email = normalizeEmail(payload.email());
        if (email == null || payload.password() == null) {
            throw new ValidationException(ValidationException.INVALID_PAYLOAD, "Credenciales invalidas");
        }
        CitizenUser user = citizenUserRepository.findByEmail(email)
                .filter(candidate -> passwordEncoder.matches(payload.password(), candidate.getPasswordHash()))
                .orElseThrow(() -> new UnauthorizedException(UnauthorizedException.INVALID_CREDENTIALS,
                        "Email o password invalidos"));
        return issue(user);
    }

    /** Sliding session: token nuevo con los datos actuales del usuario (nombre/email pueden haber cambiado). */
    @Transactional(readOnly = true)
    public AuthResponse refresh(AuthenticatedUser principal) {
        return issue(citizenUserService.getCurrent(principal));
    }

    public static String normalizeEmail(String email) {
        String trimmed = TextNormalizer.trimToNull(email);
        return trimmed == null ? null : trimmed.toLowerCase(Locale.ROOT);
    }

    public static boolean isValidEmail(String email) {
        return email != null && email.length() <= 255 && EMAIL.matcher(email).matches();
    }

    static ConflictException emailTaken() {
        return new ConflictException(ConflictException.EMAIL_ALREADY_REGISTERED, "Ya existe una cuenta con ese email");
    }

    private AuthResponse issue(CitizenUser user) {
        return AuthResponse.of(user, sessionTokenService.issue(user), sessionTokenService.getTtlSeconds());
    }
}
