package io.github.dajoh2062.traveldb.service;

import io.github.dajoh2062.traveldb.api.InvalidJourneyRequestException.FieldViolation;

import java.util.List;
import java.util.Locale;
import java.util.Set;

final class ValidationSupport {

    private ValidationSupport() {}

    static String validateOption(
            String value,
            String field,
            String defaultValue,
            Set<String> allowedValues,
            List<FieldViolation> violations
    ) {
        String normalized = normalize(value);
        if (normalized == null) {
            return defaultValue;
        }
        if (!allowedValues.contains(normalized)) {
            violations.add(new FieldViolation(field, "Select a supported value."));
            return defaultValue;
        }
        return normalized;
    }

    static String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim().toUpperCase(Locale.ROOT);
    }

    static String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
