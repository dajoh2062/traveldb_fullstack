package io.github.dajoh2062.traveldb.service;

import org.springframework.stereotype.Service;
import io.github.dajoh2062.traveldb.model.Country;
import io.github.dajoh2062.traveldb.repository.CountryRepository;

import java.util.List;

@Service
public class CountryService {

    private final CountryRepository repository;

    public CountryService(CountryRepository repository) {
        this.repository = repository;
    }

    public List<Country> listCountries() {
        return repository.findAll();
    }

    public boolean countryExists(String countryCode) {
        if (countryCode == null || countryCode.isBlank()) {
            return false;
        }
        return repository.existsByCountryCode(countryCode);
    }
}
