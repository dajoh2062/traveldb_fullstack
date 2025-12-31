package projects.traveldbbackend;

import java.util.List;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "http://localhost:5173")
public class TravelController {

    private final TravelService service;

    public TravelController(TravelService service) {
        this.service = service;
    }

    @PostMapping("/journey/check")
    public TravelService.JourneyResponse check(@RequestBody TravelService.JourneyRequest req) {
        return service.checkJourney(req);
    }
    @GetMapping("/airports/search")
    public List<Airport> searchAirports(@RequestParam String q) {
        if (q == null || q.length() < 2) {
            return List.of();
        }
        return service.searchAirports(q);
}

}
