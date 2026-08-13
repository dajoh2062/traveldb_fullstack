package io.github.dajoh2062.traveldb.api.dto;

import io.github.dajoh2062.traveldb.documents.DocumentCheckResult;
import io.github.dajoh2062.traveldb.baggage.BaggageAdvice;

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
