package projects.traveldbbackend;

import java.util.List;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class TravelController {

    private final TravelService service;

    public TravelController(TravelService service) {
        this.service = service;
    }

    @PostMapping("/journey/check")
    public TravelService.JourneyResponse check(@RequestBody(required = false) TravelService.JourneyRequest req) {
        return service.checkJourney(req);
    }
    @GetMapping("/airports/search")
    public TravelService.AirportSearchResponse searchAirports(
            @RequestParam String q,
            @RequestParam(defaultValue = "0") int offset,
            @RequestParam(defaultValue = "50") int limit
    ) {
        if (q == null || q.isBlank()) {
            return new TravelService.AirportSearchResponse(List.of(), 0, 0, limit, false);
        }
        return service.searchAirports(q, offset, limit);
    }

    @GetMapping("/countries")
    public List<Country> countries() {
        return service.getCountries();
    }

}
