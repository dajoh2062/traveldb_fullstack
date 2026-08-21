package io.github.dajoh2062.traveldb.service;

import io.github.dajoh2062.traveldb.repository.DatabaseHealthRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;

@Service
public class HealthService {

    private static final Logger log = LoggerFactory.getLogger(HealthService.class);

    private final DatabaseHealthRepository repository;

    public HealthService(DatabaseHealthRepository repository) {
        this.repository = repository;
    }

    public boolean databaseAvailable() {
        try {
            return repository.isAvailable();
        } catch (DataAccessException error) {
            log.warn("Database health check failed: {}", error.getMessage());
            return false;
        }
    }
}
