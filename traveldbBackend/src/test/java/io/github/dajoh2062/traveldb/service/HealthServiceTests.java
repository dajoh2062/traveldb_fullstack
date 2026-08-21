package io.github.dajoh2062.traveldb.service;

import io.github.dajoh2062.traveldb.repository.DatabaseHealthRepository;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessResourceFailureException;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class HealthServiceTests {

    private final DatabaseHealthRepository repository = mock(DatabaseHealthRepository.class);
    private final HealthService service = new HealthService(repository);

    @Test
    void reportsAvailableWhenTheDatabaseResponds() {
        when(repository.isAvailable()).thenReturn(true);

        assertTrue(service.databaseAvailable());
    }

    @Test
    void reportsUnavailableWhenTheDatabaseCannotBeReached() {
        when(repository.isAvailable()).thenThrow(new DataAccessResourceFailureException("offline"));

        assertFalse(service.databaseAvailable());
    }
}
