package projects.traveldbbackend.rules;

import projects.traveldbbackend.Airport;

import java.util.List;

public record RuleContext(
        String nationality,
        List<Airport> route
) {
    public Airport origin() {
        return route.get(0);
    }

    public Airport destination() {
        return route.get(route.size() - 1);
    }

    public boolean touchesCountry(String countryCode2) {
        return route.stream()
                .anyMatch(a -> countryCode2.equalsIgnoreCase(a.getCountryCode()));
    }
}
