package io.github.dajoh2062.traveldb.api;

import io.github.dajoh2062.traveldb.service.HealthService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/health")
public class HealthController {

    private final HealthService healthService;

    public HealthController(HealthService healthService) {
        this.healthService = healthService;
    }

    @GetMapping
    public ResponseEntity<Map<String, String>> health() {
        boolean databaseAvailable = healthService.databaseAvailable();
        return ResponseEntity
                .status(databaseAvailable ? HttpStatus.OK : HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of("status", databaseAvailable ? "ok" : "unavailable"));
    }
}
