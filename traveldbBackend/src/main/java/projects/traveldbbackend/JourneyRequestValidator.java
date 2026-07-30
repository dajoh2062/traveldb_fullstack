package projects.traveldbbackend;

import org.springframework.stereotype.Component;
import projects.traveldbbackend.api.InvalidJourneyRequestException;
import projects.traveldbbackend.api.InvalidJourneyRequestException.FieldViolation;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

@Component
public class JourneyRequestValidator {

    static final int MAX_ROUTE_AIRPORTS = 20;
    private static final int MAX_COUNTRY_LIST_SIZE = 50;
    private static final Pattern COUNTRY_CODE = Pattern.compile("[A-Z]{2}");
    private static final Pattern AIRPORT_CODE = Pattern.compile("[A-Z]{3}");

    private final TravelRepository repository;

    public JourneyRequestValidator(TravelRepository repository) {
        this.repository = repository;
    }

    public ValidatedJourney validate(TravelService.JourneyRequest request) {
        if (request == null) {
            throw new InvalidJourneyRequestException(List.of(
                    new FieldViolation("request", "Request body must be a JSON object.")
            ));
        }

        List<FieldViolation> violations = new ArrayList<>();
        String nationality = validateCountryCode(
                request.nationalityCountryCode(),
                "nationalityCountryCode",
                true,
                violations
        );
        List<Airport> airports = validateRoute(request.route(), violations);

        TravelService.DocumentOptions rawDocuments = request.documents() == null
                ? TravelService.DocumentOptions.defaults()
                : request.documents();
        TravelService.DocumentOptions documents = validateDocuments(rawDocuments, violations);
        TravelService.BaggageOptions baggage = request.baggage() == null
                ? TravelService.BaggageOptions.defaults()
                : request.baggage();

        if (!violations.isEmpty()) {
            throw new InvalidJourneyRequestException(violations);
        }

        return new ValidatedJourney(nationality, List.copyOf(airports), baggage, documents);
    }

    private List<Airport> validateRoute(List<String> route, List<FieldViolation> violations) {
        if (route == null) {
            violations.add(new FieldViolation("route", "Route is required."));
            return List.of();
        }
        if (route.size() < 2) {
            violations.add(new FieldViolation("route", "Route must contain at least an origin and a destination."));
        }
        if (route.size() > MAX_ROUTE_AIRPORTS) {
            violations.add(new FieldViolation(
                    "route",
                    "Route cannot contain more than " + MAX_ROUTE_AIRPORTS + " airports."
            ));
        }

        List<Airport> airports = new ArrayList<>();
        int valuesToValidate = Math.min(route.size(), MAX_ROUTE_AIRPORTS);
        for (int index = 0; index < valuesToValidate; index++) {
            String field = "route[" + index + "]";
            String code = normalize(route.get(index));
            if (code == null || !AIRPORT_CODE.matcher(code).matches()) {
                violations.add(new FieldViolation(field, "Must be a three-letter IATA airport code."));
                continue;
            }
            repository.findAirport(code).ifPresentOrElse(
                    airports::add,
                    () -> violations.add(new FieldViolation(field, "Unknown airport code: " + code + "."))
            );
        }
        return airports;
    }

    private TravelService.DocumentOptions validateDocuments(
            TravelService.DocumentOptions documents,
            List<FieldViolation> violations
    ) {
        String residenceCountry = validateCountryCode(
                documents.residenceCountryCode(),
                "documents.residenceCountryCode",
                false,
                violations
        );
        String passportCountry = validateCountryCode(
                documents.passportIssuingCountryCode(),
                "documents.passportIssuingCountryCode",
                false,
                violations
        );
        List<String> residencePermits = validateCountryCodeList(
                documents.residencePermitCountryCodes(),
                "documents.residencePermitCountryCodes",
                violations
        );
        List<String> visas = validateCountryCodeList(
                documents.visaCountryCodes(),
                "documents.visaCountryCodes",
                violations
        );

        LocalDate today = LocalDate.now();
        LocalDate departureDate = documents.departureDate();
        LocalDate passportExpiryDate = documents.passportExpiryDate();
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

        Integer age = documents.travelerAge();
        if (age != null && (age < 0 || age > 120)) {
            violations.add(new FieldViolation(
                    "documents.travelerAge",
                    "Traveler age must be between 0 and 120."
            ));
        }

        String purpose = normalize(documents.travelPurpose());
        return new TravelService.DocumentOptions(
                residenceCountry,
                passportCountry,
                passportExpiryDate,
                departureDate,
                purpose,
                age,
                residencePermits,
                visas
        );
    }

    private List<String> validateCountryCodeList(
            List<String> values,
            String field,
            List<FieldViolation> violations
    ) {
        if (values == null) return List.of();
        if (values.size() > MAX_COUNTRY_LIST_SIZE) {
            violations.add(new FieldViolation(
                    field,
                    "Cannot contain more than " + MAX_COUNTRY_LIST_SIZE + " country codes."
            ));
        }

        Set<String> normalizedValues = new LinkedHashSet<>();
        int valuesToValidate = Math.min(values.size(), MAX_COUNTRY_LIST_SIZE);
        for (int index = 0; index < valuesToValidate; index++) {
            String normalized = validateCountryCode(
                    values.get(index),
                    field + "[" + index + "]",
                    true,
                    violations
            );
            if (normalized != null) normalizedValues.add(normalized);
        }
        return List.copyOf(normalizedValues);
    }

    private String validateCountryCode(
            String value,
            String field,
            boolean required,
            List<FieldViolation> violations
    ) {
        String normalized = normalize(value);
        if (normalized == null) {
            if (required) violations.add(new FieldViolation(field, "Country code is required."));
            return null;
        }
        if (!COUNTRY_CODE.matcher(normalized).matches()) {
            violations.add(new FieldViolation(field, "Must be a two-letter ISO country code."));
            return null;
        }
        if (!repository.countryExists(normalized)) {
            violations.add(new FieldViolation(field, "Unknown country code: " + normalized + "."));
            return null;
        }
        return normalized;
    }

    private String normalize(String value) {
        if (value == null || value.isBlank()) return null;
        return value.trim().toUpperCase(Locale.ROOT);
    }

    public record ValidatedJourney(
            String nationalityCountryCode,
            List<Airport> airports,
            TravelService.BaggageOptions baggage,
            TravelService.DocumentOptions documents
    ) {}
}
