package com.darmoz.tramitea.exception;

/** 401 con el mismo codigo que usan los route handlers del front. */
public class UnauthorizedException extends RuntimeException {

    public static final String UNAUTHORIZED = "unauthorized";
    public static final String INVALID_CREDENTIALS = "invalid-credentials";

    private final String code;

    public UnauthorizedException() {
        this(UNAUTHORIZED, "Sesion invalida");
    }

    public UnauthorizedException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
