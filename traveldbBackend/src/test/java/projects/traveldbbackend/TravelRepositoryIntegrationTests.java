package projects.traveldbbackend;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import projects.traveldbbackend.model.Airport;
import projects.traveldbbackend.repository.TravelRepository;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
class TravelRepositoryIntegrationTests {

    @Autowired
    private TravelRepository repository;

    @Test
    void exactIataCodeIsRankedFirst() {
        assertEquals("JFK", repository.searchAirports("jfk").getFirst().getIataCode());
    }

    @Test
    void searchesByStructuredCityCountryKeywordAndUnaccentedName() {
        assertTrue(repository.searchAirports("London").stream()
                .anyMatch(airport -> airport.getIataCode().equals("LHR")));
        assertEquals("BOS", repository.searchAirports("Boston").getFirst().getIataCode());
        assertEquals("Boston", repository.searchAirports("Boston").getFirst().getCity());
        assertTrue(repository.searchAirports("Chicago").stream()
                .anyMatch(airport -> airport.getIataCode().equals("ORD")));
        assertTrue(repository.searchAirports("Chicago").stream()
                .anyMatch(airport -> airport.getIataCode().equals("MDW")));
        assertEquals("CGK", repository.searchAirports("Jakarta").getFirst().getIataCode());
        assertTrue(repository.searchAirports("Norway").stream()
                .anyMatch(airport -> airport.getIataCode().equals("OSL")));
        assertTrue(repository.searchAirports("Malaga").stream()
                .anyMatch(airport -> airport.getIataCode().equals("AGP")));
        assertEquals("JFK", repository.searchAirports("KJFK").getFirst().getIataCode());
        assertEquals("JFK", repository.searchAirports("Idlewild").getFirst().getIataCode());
    }

    @Test
    void exposesCountriesForNationalitySearch() {
        assertTrue(repository.getCountries().size() >= 240);
        assertTrue(repository.getCountries().stream()
                .anyMatch(country -> country.getCountryId().equals("NO")
                        && country.getCountryNameEn().equals("Norway")));
        assertTrue(repository.getCountries().stream()
                .anyMatch(country -> country.getCountryId().equals("BG") && country.isSchengen()));
        assertTrue(repository.getCountries().stream()
                .anyMatch(country -> country.getCountryId().equals("RO") && country.isSchengen()));
    }

    @Test
    void exposesProfessionalAirportMetadataAcrossRegions() {
        for (String code : List.of("OSL", "JFK", "GRU", "JNB", "NRT", "SYD", "DXB")) {
            Airport airport = repository.getAirport(code);
            assertEquals(code, airport.getIataCode());
            assertNotNull(airport.getName());
            assertNotNull(airport.getCountryCode());
            assertNotNull(airport.getContinent());
            assertTrue(airport.getLatitude() >= -90 && airport.getLatitude() <= 90);
            assertTrue(airport.getLongitude() >= -180 && airport.getLongitude() <= 180);
        }

        Airport jfk = repository.getAirport("JFK");
        assertTrue(jfk.getSourceId() > 0);
        assertEquals("KJFK", jfk.getIdent());
        assertEquals("KJFK", jfk.getIcaoCode());
        assertEquals("JFK", jfk.getLocalCode());
        assertEquals("New York", jfk.getCity());
        assertTrue(jfk.isScheduledService());
        assertNotNull(jfk.getOfficialUrl());
    }
}
