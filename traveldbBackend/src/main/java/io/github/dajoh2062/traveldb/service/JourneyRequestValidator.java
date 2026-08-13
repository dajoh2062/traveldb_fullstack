package io.github.dajoh2062.traveldb.service;

import org.springframework.stereotype.Component;
import io.github.dajoh2062.traveldb.api.InvalidJourneyRequestException;
import io.github.dajoh2062.traveldb.api.InvalidJourneyRequestException.FieldViolation;
import io.github.dajoh2062.traveldb.api.dto.BaggageOptions;
import io.github.dajoh2062.traveldb.api.dto.DocumentOptions;
import io.github.dajoh2062.traveldb.api.dto.JourneyRequest;
import io.github.dajoh2062.traveldb.model.Airport;

import java.util.ArrayList;
import java.util.List;

@Component
public class JourneyRequestValidator {

    public static final int MAX_ROUTE_AIRPORTS = RouteValidator.MAX_ROUTE_AIRPORTS;
    public static final int MAX_TRAVEL_DOCUMENTS = DocumentOptionsValidator.MAX_TRAVEL_DOCUMENTS;

    private final CountryCodeValidator countryCodeValidator;
    private final RouteValidator routeValidator;
    private final DocumentOptionsValidator documentOptionsValidator;
    private final BaggageOptionsValidator baggageOptionsValidator;

    public JourneyRequestValidator(
            CountryCodeValidator countryCodeValidator,
            RouteValidator routeValidator,
            DocumentOptionsValidator documentOptionsValidator,
            BaggageOptionsValidator baggageOptionsValidator
    ) {
        this.countryCodeValidator = countryCodeValidator;
        this.routeValidator = routeValidator;
        this.documentOptionsValidator = documentOptionsValidator;
        this.baggageOptionsValidator = baggageOptionsValidator;
    }

    public ValidatedJourney validate(JourneyRequest request) {
        if (request == null) {
            throw new InvalidJourneyRequestException(List.of(
                    new FieldViolation("request", "Request body must be a JSON object.")
            ));
        }

        List<FieldViolation> violations = new ArrayList<>();
        String nationality = countryCodeValidator.validate(
                request.nationalityCountryCode(),
                "nationalityCountryCode",
                true,
                violations
        );
        List<Airport> airports = routeValidator.validate(request.route(), violations);
        DocumentOptions documents = documentOptionsValidator.validate(request.documents(), violations);
        BaggageOptions baggage = baggageOptionsValidator.validate(request.baggage(), violations);

        if (!violations.isEmpty()) {
            throw new InvalidJourneyRequestException(violations);
        }

        return new ValidatedJourney(nationality, airports, baggage, documents);
    }

    public record ValidatedJourney(
            String nationalityCountryCode,
            List<Airport> airports,
            BaggageOptions baggage,
            DocumentOptions documents
    ) {}
}
