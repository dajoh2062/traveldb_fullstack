package projects.traveldbbackend.api;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import projects.traveldbbackend.api.dto.AirportSearchResponse;
import projects.traveldbbackend.api.dto.JourneyRequest;
import projects.traveldbbackend.api.dto.JourneyResponse;
import projects.traveldbbackend.model.Country;
import projects.traveldbbackend.service.TravelService;

import java.util.List;

@RestController
@RequestMapping("/api")
public class TravelController {

    private static final String DEFAULT_SEARCH_OFFSET = "0";
    private static final String DEFAULT_SEARCH_LIMIT = "50";

    private final TravelService travelService;

    public TravelController(TravelService travelService) {
        this.travelService = travelService;
    }

    @PostMapping("/journey/check")
    public JourneyResponse checkJourney(@RequestBody(required = false) JourneyRequest request) {
        return travelService.checkJourney(request);
    }

    @GetMapping("/airports/search")
    public AirportSearchResponse searchAirports(
            @RequestParam String q,
            @RequestParam(defaultValue = DEFAULT_SEARCH_OFFSET) int offset,
            @RequestParam(defaultValue = DEFAULT_SEARCH_LIMIT) int limit
    ) {
        if (q == null || q.isBlank()) {
            return new AirportSearchResponse(List.of(), 0, 0, limit, false);
        }
        return travelService.searchAirports(q, offset, limit);
    }

    @GetMapping("/countries")
    public List<Country> listCountries() {
        return travelService.getCountries();
    }
}
