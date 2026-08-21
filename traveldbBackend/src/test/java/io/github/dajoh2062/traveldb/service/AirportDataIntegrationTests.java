package io.github.dajoh2062.traveldb.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import io.github.dajoh2062.traveldb.model.Airport;
import io.github.dajoh2062.traveldb.model.Country;
import io.github.dajoh2062.traveldb.repository.AirportRepository;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
class AirportDataIntegrationTests {

    @Autowired
    private AirportRepository airportRepository;

    @Autowired
    private AirportSearchService airportSearchService;

    @Autowired
    private CountryService countryService;

    @Test
    void exactIataCodeIsRankedFirst() {
        assertEquals("JFK", airportSearchService.rankedMatches("jfk").getFirst().iataCode());
    }

    @Test
    void reusesRankedResultsForEquivalentQueries() {
        List<Airport> firstSearch = airportSearchService.rankedMatches("London");

        assertSame(firstSearch, airportSearchService.rankedMatches("  LONDON  "));
        assertSame(firstSearch, airportSearchService.rankedMatches("Lóndon"));
    }

    @Test
    void expiresOldSearchesInsteadOfGrowingTheCacheWithoutLimit() {
        List<Airport> firstSearch = airportSearchService.rankedMatches("London");

        for (int index = 0; index < 128; index++) {
            airportSearchService.rankedMatches("cache-capacity-probe-" + index);
        }

        assertNotSame(firstSearch, airportSearchService.rankedMatches("London"));
    }

    @Test
    void searchesByStructuredCityCountryKeywordAndUnaccentedName() {
        assertTrue(airportSearchService.rankedMatches("London").stream()
                .anyMatch(airport -> airport.iataCode().equals("LHR")));
        assertEquals("BOS", airportSearchService.rankedMatches("Boston").getFirst().iataCode());
        assertEquals("Boston", airportSearchService.rankedMatches("Boston").getFirst().city());
        assertTrue(airportSearchService.rankedMatches("Chicago").stream()
                .anyMatch(airport -> airport.iataCode().equals("ORD")));
        assertTrue(airportSearchService.rankedMatches("Chicago").stream()
                .anyMatch(airport -> airport.iataCode().equals("MDW")));
        assertEquals("CGK", airportSearchService.rankedMatches("Jakarta").getFirst().iataCode());
        assertTrue(airportSearchService.rankedMatches("Norway").stream()
                .anyMatch(airport -> airport.iataCode().equals("OSL")));
        assertTrue(airportSearchService.rankedMatches("Malaga").stream()
                .anyMatch(airport -> airport.iataCode().equals("AGP")));
        assertEquals("JFK", airportSearchService.rankedMatches("KJFK").getFirst().iataCode());
        assertEquals("JFK", airportSearchService.rankedMatches("Idlewild").getFirst().iataCode());
    }

    @Test
    void exposesCountriesForNationalitySearch() {
        List<Country> countries = countryService.listCountries();
        List<Country> reloadedCountries = countryService.listCountries();

        assertEquals(countries, reloadedCountries);
        assertNotSame(countries, reloadedCountries);
        assertTrue(countries.size() >= 240);
        assertTrue(countries.stream()
                .anyMatch(country -> country.countryId().equals("NO")
                        && country.countryNameEn().equals("Norway")));
        assertTrue(countries.stream()
                .anyMatch(country -> country.countryId().equals("BG") && country.schengen()));
        assertTrue(countries.stream()
                .anyMatch(country -> country.countryId().equals("RO") && country.schengen()));
    }

    @Test
    void exposesAirportMetadataAcrossRegions() {
        for (String code : List.of("OSL", "JFK", "GRU", "JNB", "NRT", "SYD", "DXB")) {
            Airport airport = requiredAirport(code);
            assertEquals(code, airport.iataCode());
            assertNotNull(airport.name());
            assertNotNull(airport.countryCode());
            assertNotNull(airport.continent());
            assertTrue(airport.latitude() >= -90 && airport.latitude() <= 90);
            assertTrue(airport.longitude() >= -180 && airport.longitude() <= 180);
        }

        Airport jfk = requiredAirport("JFK");
        assertTrue(jfk.sourceId() > 0);
        assertEquals("KJFK", jfk.ident());
        assertEquals("KJFK", jfk.icaoCode());
        assertEquals("JFK", jfk.localCode());
        assertEquals("New York", jfk.city());
        assertTrue(jfk.scheduledService());
        assertNotNull(jfk.officialUrl());
    }

    private Airport requiredAirport(String code) {
        return airportRepository.findByIataCode(code).orElseThrow();
    }
}
