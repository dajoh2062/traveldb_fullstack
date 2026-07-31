package projects.traveldbbackend.api.dto;

import projects.traveldbbackend.documents.DocumentCheckResult;
import projects.traveldbbackend.rules.BaggageAdvice;

import java.util.List;

public record JourneyResponse(
        boolean pickupRequired,
        List<String> pickupAt,
        List<String> documentActions,
        DocumentCheckResult documentCheck,
        List<BaggageAdvice> baggageStops,
        String baggageGuidanceReviewed,
        List<String> assumptions,
        List<String> notes
) {}
