package projects.traveldbbackend;

import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class TravelService {

    private final TravelRepository repo;

    public TravelService(TravelRepository repo) {
        this.repo = repo;
    }

    // Request DTO
    public record JourneyRequest(
        String nationalityCountryCode, // e.g. "NO"
        List<String> route             // e.g. ["OSL","FRA","JFK"]
    ) {}

    // Response DTO
    public record JourneyResponse(
        boolean pickupRequired,
        List<String> pickupAt,
        List<String> requiredDocuments
    ) {}

    public JourneyResponse checkJourney(JourneyRequest req) {
        if (req.route() == null || req.route().size() < 2) {
            return new JourneyResponse(false, List.of(), List.of("PASSPORT"));
        }

        List<Airport> airports = req.route().stream().map(repo::getAirport).toList();

        // --- BAGGAGE PICKUP RULES (simple + realistic starter) ---
        // Pickup if Schengen border changes at any transit
        // Pickup if entering USA at first US airport (simplified rule)
        List<String> pickupAt = new ArrayList<>();
        for (int i = 1; i < airports.size() - 1; i++) {
            Airport prev = airports.get(i - 1);
            Airport transit = airports.get(i);

            boolean schengenChanged = prev.isSchengen() != transit.isSchengen();
            boolean enteringUsa = !"US".equalsIgnoreCase(prev.getCountryCode())
                               && "US".equalsIgnoreCase(transit.getCountryCode());

            if (schengenChanged || enteringUsa) {
                pickupAt.add(transit.getIataCode());
            }
        }

        boolean pickupRequired = !pickupAt.isEmpty();

        // --- DOCUMENT RULES (simple starter) ---
        // Always PASSPORT.
        // If destination is Schengen and nationality is not Schengen -> VISA.
        // If destination is USA and nationality not US -> ESTA_OR_VISA.
        Airport destination = airports.get(airports.size() - 1);
        Set<String> docs = new LinkedHashSet<>();
        docs.add("PASSPORT");

        boolean nationalityIsSchengen = isSchengenNationality(req.nationalityCountryCode());
        boolean destIsSchengen = destination.isSchengen();

        if (destIsSchengen && !nationalityIsSchengen) {
            docs.add("SCHENGEN_VISA_OR_RESIDENCE_PERMIT");
        }

        if ("US".equalsIgnoreCase(destination.getCountryCode())
                && !"US".equalsIgnoreCase(req.nationalityCountryCode())) {
            docs.add("ESTA_OR_US_VISA");
        }

        return new JourneyResponse(pickupRequired, pickupAt, new ArrayList<>(docs));
    }

    // Minimal mapping (extend later or store in DB)
    private boolean isSchengenNationality(String countryCode2) {
        if (countryCode2 == null) return false;
        return Set.of("SE","DK","FI","DE","FR","ES","IT","NL","BE","PT","AT","CH","IS","NO").contains(countryCode2.toUpperCase());
    }
}
