package com.darmoz.tramitea.util;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;

/** Misma normalizacion que el front: sin diacriticos y en minusculas. */
public final class TextNormalizer {

    private static final Pattern DIACRITICS = Pattern.compile("\\p{M}+");

    private TextNormalizer() {
    }

    public static String normalize(String text) {
        if (text == null) {
            return "";
        }
        String decomposed = Normalizer.normalize(text, Normalizer.Form.NFD);
        return DIACRITICS.matcher(decomposed).replaceAll("").toLowerCase(Locale.ROOT);
    }

    /** Recorta; devuelve null si queda vacio (los opcionales del contrato). */
    public static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
