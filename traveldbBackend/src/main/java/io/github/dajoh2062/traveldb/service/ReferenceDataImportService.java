package io.github.dajoh2062.traveldb.service;

import io.github.dajoh2062.traveldb.repository.ReferenceDataRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReferenceDataImportService {

    private final ReferenceDataRepository repository;
    private final Resource countriesResource;
    private final Resource airportsResource;

    public ReferenceDataImportService(
            ReferenceDataRepository repository,
            @Value("classpath:data/countries.sql") Resource countriesResource,
            @Value("classpath:data/airports.sql") Resource airportsResource
    ) {
        this.repository = repository;
        this.countriesResource = countriesResource;
        this.airportsResource = airportsResource;
    }

    @Transactional
    public void importMissingReferenceData() {
        if (repository.countriesAreEmpty()) {
            repository.executeImport(countriesResource);
        }
        if (repository.airportsAreEmpty()) {
            repository.executeImport(airportsResource);
        }
    }
}
