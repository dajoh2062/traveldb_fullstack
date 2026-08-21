package io.github.dajoh2062.traveldb.api;

import io.github.dajoh2062.traveldb.api.dto.CountryResponse;
import io.github.dajoh2062.traveldb.service.CountryService;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.util.List;

@RestController
@RequestMapping("/api/countries")
public class CountryController {

    private static final CacheControl COUNTRY_CACHE = CacheControl.maxAge(Duration.ofHours(1)).cachePublic();

    private final CountryService countryService;

    public CountryController(CountryService countryService) {
        this.countryService = countryService;
    }

    @GetMapping
    public ResponseEntity<List<CountryResponse>> listCountries() {
        return ResponseEntity.ok()
                .cacheControl(COUNTRY_CACHE)
                .header("Vercel-CDN-Cache-Control", "public, max-age=3600")
                .body(countryService.listCountries().stream().map(CountryResponse::from).toList());
    }
}
