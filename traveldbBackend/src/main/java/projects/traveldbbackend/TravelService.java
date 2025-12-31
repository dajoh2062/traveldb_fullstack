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
    Airport destination = airports.get(airports.size() - 1);

    String nat = req.nationalityCountryCode() == null ? "" : req.nationalityCountryCode().trim().toUpperCase();

    // --- BAGGAGE PICKUP (improved baseline) ---
    // Rule: First time you arrive in the US on an itinerary, assume baggage reclaim + recheck.
    // (This matches typical US international arrival process for onward flights.)
    List<String> pickupAt = new ArrayList<>();
    for (int i = 1; i < airports.size(); i++) {
        Airport prev = airports.get(i - 1);
        Airport cur = airports.get(i);

        boolean enteringUS = !"US".equalsIgnoreCase(prev.getCountryCode())
                && "US".equalsIgnoreCase(cur.getCountryCode());

        // Only count it if it's not the final destination airport
        if (enteringUS && i < airports.size() - 1) {
            pickupAt.add(cur.getIataCode());
        }
    }

    boolean pickupRequired = !pickupAt.isEmpty();

    // --- DOCUMENTS (improved baseline) ---
    // Always passport.
    // Destination Schengen: if nationality not Schengen => Schengen visa/residence permit (simplified).
    // US: if destination OR transit-in-US => ESTA/visa/green card (simplified and explained in UI later).
    // UK/NZ/AU: require ETA/NZeTA/ETA for certain nationals, but we can’t map all nationals yet => return as conditional.
    Set<String> docs = new LinkedHashSet<>();
    docs.add("PASSPORT");

    boolean natIsSchengen = isSchengenNationalityFromDb(nat);

    if (destination.isSchengen() && !natIsSchengen) {
        docs.add("SCHENGEN_VISA_OR_RESIDENCE_PERMIT");
    }

    boolean itineraryTouchesUS = airports.stream().anyMatch(a -> "US".equalsIgnoreCase(a.getCountryCode()));
    if (itineraryTouchesUS && !"US".equalsIgnoreCase(nat)) {
        // Covers US as destination or transit.
        docs.add("US_ENTRY_PERMISSION_ESTА_OR_VISA_OR_GREENCARD");
    }

    boolean itineraryTouchesUK = airports.stream().anyMatch(a -> "GB".equalsIgnoreCase(a.getCountryCode()));
    if (itineraryTouchesUK && !"GB".equalsIgnoreCase(nat) && !"IE".equalsIgnoreCase(nat)) {
        docs.add("UK_ETA_OR_UK_VISA_DEPENDING_ON_NATIONALITY_AND_TRANSIT_TYPE");
    }

    boolean itineraryTouchesAU = airports.stream().anyMatch(a -> "AU".equalsIgnoreCase(a.getCountryCode()));
    if (itineraryTouchesAU && !"AU".equalsIgnoreCase(nat)) {
        docs.add("AU_ETA_OR_AU_VISA_DEPENDING_ON_NATIONALITY_AND_TRANSIT_TYPE");
    }

    boolean itineraryTouchesNZ = airports.stream().anyMatch(a -> "NZ".equalsIgnoreCase(a.getCountryCode()));
    if (itineraryTouchesNZ && !"NZ".equalsIgnoreCase(nat)) {
        docs.add("NZ_NZETA_OR_TRANSIT_VISA_DEPENDING_ON_NATIONALITY");
    }

    return new JourneyResponse(pickupRequired, pickupAt, new ArrayList<>(docs));
}

private boolean isSchengenNationalityFromDb(String countryCode2) {
    if (countryCode2 == null || countryCode2.isBlank()) return false;
    Boolean val = repo.isSchengenCountry(countryCode2);
    return Boolean.TRUE.equals(val);
}

    /* 
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
        */
    // Minimal mapping (extend later or store in DB)
   
    public List<Airport> searchAirports(String query) {
    return repo.searchAirports(query);
}

}
