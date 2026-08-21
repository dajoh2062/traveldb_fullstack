package io.github.dajoh2062.traveldb.service;

import org.springframework.stereotype.Service;
import io.github.dajoh2062.traveldb.api.dto.BaggageOptions;
import io.github.dajoh2062.traveldb.api.dto.DocumentOptions;
import io.github.dajoh2062.traveldb.api.dto.JourneyRequest;
import io.github.dajoh2062.traveldb.api.dto.JourneyResponse;
import io.github.dajoh2062.traveldb.baggage.BaggageCheckRequest;
import io.github.dajoh2062.traveldb.baggage.BaggageCheckResult;
import io.github.dajoh2062.traveldb.baggage.BaggageService;
import io.github.dajoh2062.traveldb.documents.DocumentCheckInput;
import io.github.dajoh2062.traveldb.documents.DocumentCheckResult;
import io.github.dajoh2062.traveldb.documents.DocumentRequirement;
import io.github.dajoh2062.traveldb.documents.DocumentRequirementsProvider;
import io.github.dajoh2062.traveldb.model.Airport;

import java.util.List;
import java.util.Locale;

@Service
public class JourneyService {

    private static final String ENTRY_CONDITIONS = "ENTRY_CONDITIONS";

    private final BaggageService baggageService;
    private final DocumentRequirementsProvider documentRequirements;
    private final JourneyRequestValidator requestValidator;

    public JourneyService(
            BaggageService baggageService,
            DocumentRequirementsProvider documentRequirements,
            JourneyRequestValidator requestValidator
    ) {
        this.baggageService = baggageService;
        this.documentRequirements = documentRequirements;
        this.requestValidator = requestValidator;
    }

    public JourneyResponse checkJourney(JourneyRequest request) {
        JourneyRequestValidator.ValidatedJourney journey = requestValidator.validate(request);
        List<Airport> airports = journey.airports();
        BaggageOptions baggage = journey.baggage();

        BaggageCheckResult baggageCheck = baggageService.check(new BaggageCheckRequest(
                airports,
                baggage.checkedBaggage() == null || baggage.checkedBaggage(),
                parseTicketArrangement(baggage.ticketArrangement()),
                parseThroughCheckStatus(baggage.checkedThrough())
        ));

        DocumentOptions documents = journey.documents();
        DocumentCheckResult documentCheck = documentRequirements.check(new DocumentCheckInput(
                journey.nationalityCountryCode(),
                airports,
                documents.residenceCountryCode(),
                documents.passportIssuingCountryCode(),
                documents.passportExpiryDate(),
                documents.departureDate(),
                documents.travelPurpose(),
                documents.travelerAge(),
                documents.residencePermitCountryCodes(),
                documents.visaCountryCodes(),
                List.copyOf(baggageCheck.baggagePickupAt()),
                documents.travelDocuments()
        ));

        List<String> pickupAirports = airports.stream()
                .map(Airport::iataCode)
                .filter(baggageCheck.baggagePickupAt()::contains)
                .toList();
        List<String> documentActions = documentCheck.requirements().stream()
                .filter(requirement -> requirement.status() != DocumentRequirement.Status.NOT_REQUIRED)
                .filter(requirement -> !ENTRY_CONDITIONS.equals(requirement.code()))
                .map(DocumentRequirement::code)
                .distinct()
                .toList();

        return new JourneyResponse(
                !pickupAirports.isEmpty(),
                pickupAirports,
                documentActions,
                documentCheck,
                baggageCheck.baggageAdvice(),
                baggageCheck.guidanceReviewedDate(),
                List.copyOf(baggageCheck.assumptions()),
                List.copyOf(baggageCheck.notes())
        );
    }

    private static BaggageCheckRequest.TicketArrangement parseTicketArrangement(String value) {
        try {
            return BaggageCheckRequest.TicketArrangement.valueOf(normalizeOption(value));
        } catch (IllegalArgumentException ignored) {
            return BaggageCheckRequest.TicketArrangement.UNKNOWN;
        }
    }

    private static BaggageCheckRequest.ThroughCheckStatus parseThroughCheckStatus(String value) {
        try {
            return BaggageCheckRequest.ThroughCheckStatus.valueOf(normalizeOption(value));
        } catch (IllegalArgumentException ignored) {
            return BaggageCheckRequest.ThroughCheckStatus.UNKNOWN;
        }
    }

    private static String normalizeOption(String value) {
        return value == null ? "UNKNOWN" : value.trim().toUpperCase(Locale.ROOT);
    }
}
