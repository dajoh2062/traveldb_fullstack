package io.github.dajoh2062.traveldb.api;

import io.github.dajoh2062.traveldb.api.dto.AirportSearchResponse;
import io.github.dajoh2062.traveldb.service.AirportSearchService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/airports")
public class AirportController {

    private static final String DEFAULT_SEARCH_OFFSET = "0";
    private static final String DEFAULT_SEARCH_LIMIT = "50";

    private final AirportSearchService airportSearchService;

    public AirportController(AirportSearchService airportSearchService) {
        this.airportSearchService = airportSearchService;
    }

    @GetMapping("/search")
    public AirportSearchResponse searchAirports(
            @RequestParam String q,
            @RequestParam(defaultValue = DEFAULT_SEARCH_OFFSET) int offset,
            @RequestParam(defaultValue = DEFAULT_SEARCH_LIMIT) int limit
    ) {
        return airportSearchService.searchAirports(q, offset, limit);
    }
}
