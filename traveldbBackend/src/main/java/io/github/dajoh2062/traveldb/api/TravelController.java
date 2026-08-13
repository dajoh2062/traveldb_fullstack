package io.github.dajoh2062.traveldb.api;

import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import io.github.dajoh2062.traveldb.api.dto.AirportSearchResponse;
import io.github.dajoh2062.traveldb.api.dto.CountryResponse;
import io.github.dajoh2062.traveldb.api.dto.JourneyRequest;
import io.github.dajoh2062.traveldb.api.dto.JourneyResponse;
import io.github.dajoh2062.traveldb.service.AirportSearchService;
import io.github.dajoh2062.traveldb.service.CountryService;
import io.github.dajoh2062.traveldb.service.JourneyService;

import java.time.Duration;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class TravelController {

    private static final String DEFAULT_SEARCH_OFFSET = "0";
    private static final String DEFAULT_SEARCH_LIMIT = "50";
    private static final CacheControl COUNTRY_CACHE = CacheControl.maxAge(Duration.ofHours(1)).cachePublic();

    private final JourneyService journeyService;
    private final AirportSearchService airportSearchService;
    private final CountryService countryService;

    public TravelController(
            JourneyService journeyService,
            AirportSearchService airportSearchService,
            CountryService countryService
    ) {
        this.journeyService = journeyService;
        this.airportSearchService = airportSearchService;
        this.countryService = countryService;
    }

    @PostMapping("/journey/check")
    public JourneyResponse checkJourney(@RequestBody(required = false) JourneyRequest request) {
        return journeyService.checkJourney(request);
    }

    @GetMapping("/airports/search")
    public AirportSearchResponse searchAirports(
            @RequestParam String q,
            @RequestParam(defaultValue = DEFAULT_SEARCH_OFFSET) int offset,
            @RequestParam(defaultValue = DEFAULT_SEARCH_LIMIT) int limit
    ) {
        return airportSearchService.searchAirports(q, offset, limit);
    }

    @GetMapping("/countries")
    public ResponseEntity<List<CountryResponse>> listCountries() {
        return ResponseEntity.ok()
                .cacheControl(COUNTRY_CACHE)
                .header("Vercel-CDN-Cache-Control", "public, max-age=3600")
                .body(countryService.listCountries().stream().map(CountryResponse::from).toList());
    }

    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of("status", "ok");
    }
}
