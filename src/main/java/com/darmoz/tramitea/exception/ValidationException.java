package com.darmoz.tramitea.exception;

/**
 * 400 con un codigo estable en el campo `error` de la respuesta (los mismos
 * que usa el front: `invalid-suggestion-payload`, `invalid-profile-payload`...).
 */
public class ValidationException extends RuntimeException {

    public static final String INVALID_SUGGESTION_PAYLOAD = "invalid-suggestion-payload";
    public static final String INVALID_PROFILE_PAYLOAD = "invalid-profile-payload";
    public static final String INVALID_COMMENT_PAYLOAD = "invalid-comment-payload";
    public static final String INVALID_PAYLOAD = "invalid-payload";

    private final String code;

    public ValidationException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
