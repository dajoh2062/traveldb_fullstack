package projects.traveldbbackend;

import org.springframework.stereotype.Service;
import projects.traveldbbackend.rules.RuleContext;
import projects.traveldbbackend.rules.RuleEngine;
import projects.traveldbbackend.rules.RuleResult;

import java.util.List;

@Service
public class TravelService {

    private final TravelRepository repo;
    private final RuleEngine engine;

    public TravelService(TravelRepository repo, RuleEngine engine) {
        this.repo = repo;
        this.engine = engine;
    }

    // Request DTO
    public record JourneyRequest(
            String nationalityCountryCode, // e.g. "NO"
            List<String> route              // e.g. ["OSL","FRA","JFK"]
    ) {}

    // Response DTO
    public record JourneyResponse(
            boolean pickupRequired,
            List<String> pickupAt,
            List<String> requiredDocuments,

            // Extra fields; safe for frontend to ignore.
            List<String> assumptions,
            List<String> notes
    ) {}

    public JourneyResponse checkJourney(JourneyRequest req) {
        if (req == null || req.route() == null || req.route().size() < 2) {
            return new JourneyResponse(
                    false,
                    List.of(),
                    List.of("PASSPORT"),
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

        RuleContext ctx = new RuleContext(nat, airports);
        RuleResult result = engine.evaluate(ctx);

        return new JourneyResponse(
                !result.baggagePickupAt().isEmpty(),
                result.baggagePickupAt().stream().toList(),
                result.requiredDocuments().stream().toList(),
                result.assumptions().stream().toList(),
                result.notes().stream().toList()
        );
    }

    public List<Airport> searchAirports(String query) {
        return repo.searchAirports(query);
    }
}
