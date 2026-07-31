package projects.traveldbbackend.api.dto;

import java.util.List;

public record JourneyRequest(
        String nationalityCountryCode,
        List<String> route,
        BaggageOptions baggage,
        DocumentOptions documents
) {
    public JourneyRequest(String nationalityCountryCode, List<String> route) {
        this(nationalityCountryCode, route, BaggageOptions.defaults(), DocumentOptions.defaults());
    }

    public JourneyRequest(String nationalityCountryCode, List<String> route, BaggageOptions baggage) {
        this(nationalityCountryCode, route, baggage, DocumentOptions.defaults());
    }
}
