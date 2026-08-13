package io.github.dajoh2062.traveldb.service;

import jakarta.annotation.PostConstruct;
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
    private List<Country> countries = List.of();
    private Set<String> countryCodes = Set.of();

    public CountryService(CountryRepository repository) {
        this.repository = repository;
    }

    @PostConstruct
    void loadCountries() {
        countries = repository.findAll();
        countryCodes = countries.stream()
                .map(Country::countryId)
                .collect(Collectors.toUnmodifiableSet());
    }

    public List<Country> listCountries() {
        return countries;
    }

    public boolean countryExists(String countryCode) {
        if (countryCode == null || countryCode.isBlank()) {
            return false;
        }
        return countryCodes.contains(countryCode.trim().toUpperCase(Locale.ROOT));
    }
}
