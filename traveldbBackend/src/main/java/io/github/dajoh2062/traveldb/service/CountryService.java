package io.github.dajoh2062.traveldb.service;

import org.springframework.stereotype.Service;
import io.github.dajoh2062.traveldb.model.Country;
import io.github.dajoh2062.traveldb.repository.CountryRepository;

import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class CountryService {

    private final CountryRepository repository;
    private volatile List<Country> countries;
    private volatile Set<String> countryCodes;

    public CountryService(CountryRepository repository) {
        this.repository = repository;
    }

    public List<Country> listCountries() {
        ensureLoaded();
        return countries;
    }

    public boolean countryExists(String countryCode) {
        ensureLoaded();
        if (countryCode == null || countryCode.isBlank()) {
            return false;
        }
        return countryCodes.contains(countryCode.trim().toUpperCase(Locale.ROOT));
    }

    private void ensureLoaded() {
        if (countries != null) {
            return;
        }
        synchronized (this) {
            if (countries == null) {
                List<Country> loadedCountries = repository.findAll();
                countries = List.copyOf(loadedCountries);
                countryCodes = loadedCountries.stream()
                        .map(Country::countryId)
                        .collect(Collectors.toUnmodifiableSet());
            }
        }
    }
}
