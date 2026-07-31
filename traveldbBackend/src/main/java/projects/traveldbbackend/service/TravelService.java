package projects.traveldbbackend.service;

import org.springframework.stereotype.Service;
import projects.traveldbbackend.api.dto.AirportSearchItem;
import projects.traveldbbackend.api.dto.AirportSearchResponse;
import projects.traveldbbackend.api.dto.BaggageOptions;
import projects.traveldbbackend.api.dto.DocumentOptions;
import projects.traveldbbackend.api.dto.JourneyRequest;
import projects.traveldbbackend.api.dto.JourneyResponse;
import projects.traveldbbackend.documents.DocumentCheckInput;
import projects.traveldbbackend.documents.DocumentCheckResult;
import projects.traveldbbackend.documents.DocumentRequirement;
import projects.traveldbbackend.documents.DocumentRequirementsProvider;
import projects.traveldbbackend.model.Airport;
import projects.traveldbbackend.model.Country;
import projects.traveldbbackend.repository.TravelRepository;
import projects.traveldbbackend.rules.RuleContext;
import projects.traveldbbackend.rules.RuleEngine;
import projects.traveldbbackend.rules.RuleResult;

import java.util.List;
import java.util.Locale;

@Service
public class TravelService {

    private static final int MAX_AIRPORT_SEARCH_RESULTS = 100;
    private static final String BAGGAGE_GUIDANCE_REVIEWED = "2026-07-20";
    private static final String ENTRY_CONDITIONS = "ENTRY_CONDITIONS";

    private final TravelRepository repository;
    private final RuleEngine ruleEngine;
    private final DocumentRequirementsProvider documentRequirements;
    private final JourneyRequestValidator requestValidator;

    public TravelService(
            TravelRepository repository,
            RuleEngine ruleEngine,
            DocumentRequirementsProvider documentRequirements,
            JourneyRequestValidator requestValidator
    ) {
        this.repository = repository;
        this.ruleEngine = ruleEngine;
        this.documentRequirements = documentRequirements;
        this.requestValidator = requestValidator;
    }

    public JourneyResponse checkJourney(JourneyRequest request) {
        JourneyRequestValidator.ValidatedJourney journey = requestValidator.validate(request);
        List<Airport> airports = journey.airports();
        BaggageOptions baggage = journey.baggage();

        RuleContext ruleContext = new RuleContext(
                journey.nationalityCountryCode(),
                airports,
                baggage.checkedBaggage() == null || baggage.checkedBaggage(),
                parseTicketArrangement(baggage.ticketArrangement()),
                parseThroughCheckStatus(baggage.checkedThrough())
        );
        RuleResult ruleResult = ruleEngine.evaluate(ruleContext);

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
                List.copyOf(ruleResult.baggagePickupAt())
        ));

        List<String> pickupAirports = airports.stream()
                .map(Airport::getIataCode)
                .filter(ruleResult.baggagePickupAt()::contains)
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
                ruleResult.baggageAdvice(),
                BAGGAGE_GUIDANCE_REVIEWED,
                List.copyOf(ruleResult.assumptions()),
                List.copyOf(ruleResult.notes())
        );
    }

    public AirportSearchResponse searchAirports(String query, int offset, int limit) {
        int pageOffset = Math.max(0, offset);
        int pageSize = Math.max(1, Math.min(limit, MAX_AIRPORT_SEARCH_RESULTS));
        List<Airport> matches = repository.searchAirports(query);
        int fromIndex = Math.min(pageOffset, matches.size());
        int toIndex = Math.min(fromIndex + pageSize, matches.size());

        List<AirportSearchItem> airports = matches.subList(fromIndex, toIndex).stream()
                .map(AirportSearchItem::from)
                .toList();

        return new AirportSearchResponse(
                airports,
                matches.size(),
                fromIndex,
                pageSize,
                toIndex < matches.size()
        );
    }

    public List<Country> getCountries() {
        return repository.getCountries();
    }

    private RuleContext.TicketArrangement parseTicketArrangement(String value) {
        try {
            return RuleContext.TicketArrangement.valueOf(normalizeOption(value));
        } catch (IllegalArgumentException ignored) {
            return RuleContext.TicketArrangement.UNKNOWN;
        }
    }

    private RuleContext.ThroughCheckStatus parseThroughCheckStatus(String value) {
        try {
            return RuleContext.ThroughCheckStatus.valueOf(normalizeOption(value));
        } catch (IllegalArgumentException ignored) {
            return RuleContext.ThroughCheckStatus.UNKNOWN;
        }
    }

    private String normalizeOption(String value) {
        return value == null ? "UNKNOWN" : value.trim().toUpperCase(Locale.ROOT);
    }
}
