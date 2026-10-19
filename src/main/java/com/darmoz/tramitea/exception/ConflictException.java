package com.darmoz.tramitea.exception;

/** 409 con un codigo estable en `error`. */
public class ConflictException extends RuntimeException {

    public static final String EMAIL_ALREADY_REGISTERED = "email-already-registered";

    private final String code;

    public ConflictException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
