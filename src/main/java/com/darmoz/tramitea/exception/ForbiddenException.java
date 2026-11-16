package com.darmoz.tramitea.exception;

/** 403 con un codigo estable en `error`. */
public class ForbiddenException extends RuntimeException {

    public static final String LOCATION_NOT_ALLOWED = "location-not-allowed";

    private final String code;

    public ForbiddenException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
