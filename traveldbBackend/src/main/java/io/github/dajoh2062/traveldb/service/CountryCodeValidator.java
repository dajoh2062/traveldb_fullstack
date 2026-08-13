package io.github.dajoh2062.traveldb.service;

import org.springframework.stereotype.Component;
import io.github.dajoh2062.traveldb.api.InvalidJourneyRequestException.FieldViolation;

import java.util.List;
import java.util.regex.Pattern;

@Component
class CountryCodeValidator {

    private static final Pattern COUNTRY_CODE = Pattern.compile("[A-Z]{2}");

    private final CountryService countryService;

    CountryCodeValidator(CountryService countryService) {
        this.countryService = countryService;
    }

    String validate(
            String value,
            String field,
            boolean required,
            List<FieldViolation> violations
    ) {
        String normalized = ValidationSupport.normalize(value);
        if (normalized == null) {
            if (required) {
                violations.add(new FieldViolation(field, "Country code is required."));
            }
            return null;
        }
        if (!COUNTRY_CODE.matcher(normalized).matches()) {
            violations.add(new FieldViolation(field, "Must be a two-letter ISO country code."));
            return null;
        }
        if (!countryService.countryExists(normalized)) {
            violations.add(new FieldViolation(field, "Unknown country code: " + normalized + "."));
            return null;
        }
        return normalized;
    }
}
