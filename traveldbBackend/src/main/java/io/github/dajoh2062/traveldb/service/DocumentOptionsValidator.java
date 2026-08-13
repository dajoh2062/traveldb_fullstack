package io.github.dajoh2062.traveldb.service;

import org.springframework.stereotype.Component;
import io.github.dajoh2062.traveldb.api.InvalidJourneyRequestException.FieldViolation;
import io.github.dajoh2062.traveldb.api.dto.DocumentOptions;
import io.github.dajoh2062.traveldb.api.dto.TravelDocument;
import io.github.dajoh2062.traveldb.api.dto.TravelDocument.Type;

import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Component
class DocumentOptionsValidator {

    static final int MAX_TRAVEL_DOCUMENTS = 20;

    private static final int MAX_COUNTRY_LIST_SIZE = 50;
    private static final int MAX_CUSTOM_DOCUMENT_TYPE_LENGTH = 80;
    private static final int MAX_TRAVELER_AGE = 120;
    private static final Set<String> TRAVEL_PURPOSES = Set.of(
            "TOURISM", "BUSINESS", "VISIT", "TRANSIT", "STUDY", "WORK", "OTHER"
    );

    private final CountryCodeValidator countryCodeValidator;
    private final Clock clock;

    DocumentOptionsValidator(CountryCodeValidator countryCodeValidator, Clock clock) {
        this.countryCodeValidator = countryCodeValidator;
        this.clock = clock;
    }

    DocumentOptions validate(DocumentOptions documents, List<FieldViolation> violations) {
        DocumentOptions options = documents == null ? DocumentOptions.defaults() : documents;
        String residenceCountry = countryCodeValidator.validate(
                options.residenceCountryCode(),
                "documents.residenceCountryCode",
                false,
                violations
        );
        String passportCountry = countryCodeValidator.validate(
                options.passportIssuingCountryCode(),
                "documents.passportIssuingCountryCode",
                false,
                violations
        );
        List<String> residencePermits = validateCountryCodeList(
                options.residencePermitCountryCodes(),
                "documents.residencePermitCountryCodes",
                violations
        );
        List<String> visas = validateCountryCodeList(
                options.visaCountryCodes(),
                "documents.visaCountryCodes",
                violations
        );

        LocalDate today = LocalDate.now(clock);
        LocalDate departureDate = options.departureDate();
        LocalDate passportExpiryDate = options.passportExpiryDate();
        if (departureDate != null && departureDate.isBefore(today)) {
            violations.add(new FieldViolation(
                    "documents.departureDate",
                    "Departure date cannot be in the past."
            ));
        }
        if (passportExpiryDate != null && passportExpiryDate.isBefore(today)) {
            violations.add(new FieldViolation(
                    "documents.passportExpiryDate",
                    "Passport expiry date cannot be in the past."
            ));
        } else if (passportExpiryDate != null
                && departureDate != null
                && passportExpiryDate.isBefore(departureDate)) {
            violations.add(new FieldViolation(
                    "documents.passportExpiryDate",
                    "Passport must not expire before the departure date."
            ));
        }

        List<TravelDocument> travelDocuments = validateTravelDocuments(
                options.travelDocuments(),
                departureDate,
                today,
                violations
        );
        residencePermits = mergeHeldDocumentCountries(
                residencePermits,
                travelDocuments,
                Type.RESIDENCE_PERMIT
        );
        visas = mergeHeldDocumentCountries(visas, travelDocuments, Type.VISA);

        TravelDocument primaryDocument = travelDocuments.stream()
                .filter(document -> Boolean.TRUE.equals(document.primary()))
                .findFirst()
                .orElse(null);
        if (primaryDocument != null && Type.valueOf(primaryDocument.type()) == Type.PASSPORT) {
            passportCountry = primaryDocument.issuingCountryCode();
            passportExpiryDate = primaryDocument.expiryDate();
        }

        Integer travelerAge = options.travelerAge();
        if (travelerAge != null && (travelerAge < 0 || travelerAge > MAX_TRAVELER_AGE)) {
            violations.add(new FieldViolation(
                    "documents.travelerAge",
                    "Traveler age must be between 0 and " + MAX_TRAVELER_AGE + "."
            ));
        }

        String travelPurpose = ValidationSupport.validateOption(
                options.travelPurpose(),
                "documents.travelPurpose",
                null,
                TRAVEL_PURPOSES,
                violations
        );

        return new DocumentOptions(
                residenceCountry,
                passportCountry,
                passportExpiryDate,
                departureDate,
                travelPurpose,
                travelerAge,
                residencePermits,
                visas,
                travelDocuments
        );
    }

    private List<TravelDocument> validateTravelDocuments(
            List<TravelDocument> documents,
            LocalDate departureDate,
            LocalDate today,
            List<FieldViolation> violations
    ) {
        if (documents == null) {
            return List.of();
        }
        if (documents.size() > MAX_TRAVEL_DOCUMENTS) {
            violations.add(new FieldViolation(
                    "documents.travelDocuments",
                    "Cannot contain more than " + MAX_TRAVEL_DOCUMENTS + " documents."
            ));
        }

        int primaryCount = 0;
        List<TravelDocument> normalizedDocuments = new ArrayList<>();
        int documentsToValidate = Math.min(documents.size(), MAX_TRAVEL_DOCUMENTS);
        for (int index = 0; index < documentsToValidate; index++) {
            TravelDocument document = documents.get(index);
            String field = "documents.travelDocuments[" + index + "]";
            if (document == null) {
                violations.add(new FieldViolation(field, "Document must be a JSON object."));
                continue;
            }

            if (Boolean.TRUE.equals(document.primary())) {
                primaryCount++;
                if (primaryCount > 1) {
                    violations.add(new FieldViolation(
                            field + ".primary",
                            "Only one travel document can be primary."
                    ));
                }
            }

            String normalizedType = ValidationSupport.normalize(document.type());
            Type type = parseDocumentType(normalizedType, field + ".type", violations);
            String customType = ValidationSupport.trimToNull(document.customType());
            if (type == Type.OTHER && customType == null) {
                violations.add(new FieldViolation(
                        field + ".customType",
                        "Describe the document when type is OTHER."
                ));
            }
            if (customType != null && customType.length() > MAX_CUSTOM_DOCUMENT_TYPE_LENGTH) {
                violations.add(new FieldViolation(
                        field + ".customType",
                        "Cannot be longer than " + MAX_CUSTOM_DOCUMENT_TYPE_LENGTH + " characters."
                ));
            }

            String issuingCountry = countryCodeValidator.validate(
                    document.issuingCountryCode(),
                    field + ".issuingCountryCode",
                    type != null && type.issuingCountryRequired(),
                    violations
            );
            LocalDate expiryDate = document.expiryDate();
            if (expiryDate != null && expiryDate.isBefore(today)) {
                violations.add(new FieldViolation(
                        field + ".expiryDate",
                        "Document expiry date cannot be in the past."
                ));
            } else if (expiryDate != null
                    && departureDate != null
                    && expiryDate.isBefore(departureDate)) {
                violations.add(new FieldViolation(
                        field + ".expiryDate",
                        "Document must not expire before the departure date."
                ));
            }

            if (type != null) {
                normalizedDocuments.add(new TravelDocument(
                        type.name(),
                        customType,
                        issuingCountry,
                        expiryDate,
                        Boolean.TRUE.equals(document.primary())
                ));
            }
        }

        if (!documents.isEmpty() && primaryCount == 0) {
            violations.add(new FieldViolation(
                    "documents.travelDocuments",
                    "Select exactly one primary travel document."
            ));
        }
        return List.copyOf(normalizedDocuments);
    }

    private Type parseDocumentType(
            String value,
            String field,
            List<FieldViolation> violations
    ) {
        if (value == null) {
            violations.add(new FieldViolation(field, "Document type is required."));
            return null;
        }
        try {
            return Type.valueOf(value);
        } catch (IllegalArgumentException ignored) {
            violations.add(new FieldViolation(field, "Unsupported travel document type: " + value + "."));
            return null;
        }
    }

    private List<String> mergeHeldDocumentCountries(
            List<String> legacyCountries,
            List<TravelDocument> travelDocuments,
            Type type
    ) {
        Set<String> countries = new LinkedHashSet<>(legacyCountries);
        travelDocuments.stream()
                .filter(document -> type.name().equals(document.type()))
                .map(TravelDocument::issuingCountryCode)
                .filter(countryCode -> countryCode != null)
                .forEach(countries::add);
        return List.copyOf(countries);
    }

    private List<String> validateCountryCodeList(
            List<String> values,
            String field,
            List<FieldViolation> violations
    ) {
        if (values == null) {
            return List.of();
        }
        if (values.size() > MAX_COUNTRY_LIST_SIZE) {
            violations.add(new FieldViolation(
                    field,
                    "Cannot contain more than " + MAX_COUNTRY_LIST_SIZE + " country codes."
            ));
        }

        Set<String> normalizedValues = new LinkedHashSet<>();
        int valuesToValidate = Math.min(values.size(), MAX_COUNTRY_LIST_SIZE);
        for (int index = 0; index < valuesToValidate; index++) {
            String normalized = countryCodeValidator.validate(
                    values.get(index),
                    field + "[" + index + "]",
                    true,
                    violations
            );
            if (normalized != null) {
                normalizedValues.add(normalized);
            }
        }
        return List.copyOf(normalizedValues);
    }
}
