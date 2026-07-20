package projects.traveldbbackend;

import org.springframework.stereotype.Service;
import projects.traveldbbackend.rules.RuleContext;
import projects.traveldbbackend.rules.RuleEngine;
import projects.traveldbbackend.rules.RuleResult;
import projects.traveldbbackend.rules.BaggageAdvice;
import projects.traveldbbackend.documents.DocumentCheckInput;
import projects.traveldbbackend.documents.DocumentCheckResult;
import projects.traveldbbackend.documents.DocumentRequirement;
import projects.traveldbbackend.documents.DocumentRequirementsService;

import java.time.LocalDate;
import java.util.List;

@Service
public class TravelService {

    private final TravelRepository repo;
    private final RuleEngine engine;
    private final DocumentRequirementsService documentRequirements;

    public TravelService(
            TravelRepository repo,
            RuleEngine engine,
            DocumentRequirementsService documentRequirements
    ) {
        this.repo = repo;
        this.engine = engine;
        this.documentRequirements = documentRequirements;
    }

    // Request DTO
    public record JourneyRequest(
            String nationalityCountryCode, // e.g. "NO"
            List<String> route,             // e.g. ["OSL","FRA","JFK"]
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

    public record BaggageOptions(
            Boolean checkedBaggage,
            String ticketArrangement,
            String checkedThrough
    ) {
        public static BaggageOptions defaults() {
            return new BaggageOptions(true, "UNKNOWN", "UNKNOWN");
        }
    }

    public record DocumentOptions(
            String residenceCountryCode,
            String passportIssuingCountryCode,
            LocalDate passportExpiryDate,
            LocalDate departureDate,
            String travelPurpose,
            Integer travelerAge,
            List<String> residencePermitCountryCodes,
            List<String> visaCountryCodes
    ) {
        public static DocumentOptions defaults() {
            return new DocumentOptions(null, null, null, null, null, null, List.of(), List.of());
        }
    }

    // Response DTO
    public record JourneyResponse(
            boolean pickupRequired,
            List<String> pickupAt,
            List<String> requiredDocuments,
            DocumentCheckResult documentCheck,
            List<BaggageAdvice> baggageStops,
            String baggageGuidanceReviewed,
            List<String> assumptions,
            List<String> notes
    ) {}

    public record AirportSearchItem(
            String iataCode,
            String icaoCode,
            String name,
            String city,
            String country,
            String countryCode,
            boolean scheduledService
    ) {
        private static AirportSearchItem from(Airport airport) {
            return new AirportSearchItem(
                    airport.getIataCode(),
                    airport.getIcaoCode(),
                    airport.getName(),
                    airport.getCity(),
                    airport.getCountry(),
                    airport.getCountryCode(),
                    airport.isScheduledService()
            );
        }
    }

    public record AirportSearchResponse(
            List<AirportSearchItem> airports,
            int total,
            int offset,
            int limit,
            boolean hasMore
    ) {}

    public JourneyResponse checkJourney(JourneyRequest req) {
        if (req == null || req.route() == null || req.route().size() < 2) {
            return new JourneyResponse(
                    false,
                    List.of(),
                    List.of(),
                    DocumentCheckResult.unavailable("Route must contain at least two airports."),
                    List.of(),
                    "2026-07-20",
                    List.of(),
                    List.of("Route must contain at least two airports.")
            );
        }

        List<Airport> airports = req.route().stream()
                .map(String::trim)
                .map(String::toUpperCase)
                .map(repo::getAirport)
                .toList();

        String nat = req.nationalityCountryCode() == null
                ? ""
                : req.nationalityCountryCode().trim().toUpperCase();

        BaggageOptions baggage = req.baggage() == null ? BaggageOptions.defaults() : req.baggage();
        RuleContext ctx = new RuleContext(
                nat,
                airports,
                baggage.checkedBaggage() == null || baggage.checkedBaggage(),
                parseTicketArrangement(baggage.ticketArrangement()),
                parseThroughCheckStatus(baggage.checkedThrough())
        );
        RuleResult result = engine.evaluate(ctx);
        DocumentOptions documents = req.documents() == null ? DocumentOptions.defaults() : req.documents();
        DocumentCheckResult documentCheck = documentRequirements.check(new DocumentCheckInput(
                nat,
                airports,
                normalizeCountryCode(documents.residenceCountryCode()),
                normalizeCountryCode(documents.passportIssuingCountryCode()),
                documents.passportExpiryDate(),
                documents.departureDate(),
                documents.travelPurpose(),
                documents.travelerAge(),
                safeList(documents.residencePermitCountryCodes()),
                safeList(documents.visaCountryCodes())
        ));

        // Rule evaluation can involve multiple independent baggage rules. Build
        // the response from the itinerary so every matching stop is returned in
        // the same order the traveller will encounter it.
        List<String> pickupAt = airports.stream()
                .map(Airport::getIataCode)
                .filter(result.baggagePickupAt()::contains)
                .toList();

        return new JourneyResponse(
                !pickupAt.isEmpty(),
                pickupAt,
                documentCheck.requirements().stream()
                        .filter(requirement -> requirement.status() == DocumentRequirement.Status.REQUIRED)
                        .map(DocumentRequirement::code)
                        .distinct()
                        .toList(),
                documentCheck,
                result.baggageAdvice(),
                "2026-07-20",
                result.assumptions().stream().toList(),
                result.notes().stream().toList()
        );
    }

    private String normalizeCountryCode(String value) {
        return value == null ? null : value.trim().toUpperCase();
    }

    private List<String> safeList(List<String> values) {
        if (values == null) return List.of();
        return values.stream().map(this::normalizeCountryCode).filter(value -> value != null && !value.isBlank()).toList();
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
        return value == null ? "UNKNOWN" : value.trim().toUpperCase();
    }

    public AirportSearchResponse searchAirports(String query, int offset, int limit) {
        int safeOffset = Math.max(0, offset);
        int safeLimit = Math.max(1, Math.min(limit, 100));
        List<Airport> matches = repo.searchAirports(query);
        int fromIndex = Math.min(safeOffset, matches.size());
        int toIndex = Math.min(fromIndex + safeLimit, matches.size());
        List<AirportSearchItem> page = matches.subList(fromIndex, toIndex).stream()
                .map(AirportSearchItem::from)
                .toList();

        return new AirportSearchResponse(
                page,
                matches.size(),
                fromIndex,
                safeLimit,
                toIndex < matches.size()
        );
    }

    public List<Country> getCountries() {
        return repo.getCountries();
    }
}
