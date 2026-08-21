package io.github.dajoh2062.traveldb.api;

import io.github.dajoh2062.traveldb.service.HealthService;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class HealthControllerTests {

    private final HealthService healthService = mock(HealthService.class);
    private final HealthController controller = new HealthController(healthService);

    @Test
    void returnsServiceUnavailableWhenTheDatabaseIsDown() {
        when(healthService.databaseAvailable()).thenReturn(false);

        var response = controller.health();

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, response.getStatusCode());
        assertEquals("unavailable", response.getBody().get("status"));
    }
}
