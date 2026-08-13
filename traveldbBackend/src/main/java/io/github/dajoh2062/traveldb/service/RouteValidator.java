package io.github.dajoh2062.traveldb.service;

import org.springframework.stereotype.Component;
import io.github.dajoh2062.traveldb.api.InvalidJourneyRequestException.FieldViolation;
import io.github.dajoh2062.traveldb.model.Airport;
import io.github.dajoh2062.traveldb.repository.AirportRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

@Component
class RouteValidator {

    static final int MAX_ROUTE_AIRPORTS = 20;

    private static final Pattern AIRPORT_CODE = Pattern.compile("[A-Z]{3}");

    private final AirportRepository airportRepository;

    RouteValidator(AirportRepository airportRepository) {
        this.airportRepository = airportRepository;
    }

    List<Airport> validate(List<String> route, List<FieldViolation> violations) {
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
            String code = ValidationSupport.normalize(route.get(index));
            if (code == null || !AIRPORT_CODE.matcher(code).matches()) {
                violations.add(new FieldViolation(field, "Must be a three-letter IATA airport code."));
                continue;
            }

            airportRepository.findByIataCode(code).ifPresentOrElse(
                    airports::add,
                    () -> violations.add(new FieldViolation(field, "Unknown airport code: " + code + "."))
            );
        }
        return List.copyOf(airports);
    }
}
