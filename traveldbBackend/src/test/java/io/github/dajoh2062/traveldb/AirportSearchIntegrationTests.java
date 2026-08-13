package io.github.dajoh2062.traveldb;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import io.github.dajoh2062.traveldb.api.dto.AirportSearchResponse;
import io.github.dajoh2062.traveldb.service.AirportSearchService;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
class AirportSearchIntegrationTests {

    @Autowired
    private AirportSearchService airportSearchService;

    @Test
    void paginatesWithoutHidingCountrySearchMatches() {
        int pageSize = 100;
        AirportSearchResponse firstPage = airportSearchService.searchAirports(
                "United States", 0, pageSize
        );

        assertTrue(firstPage.total() > pageSize);
        assertEquals(pageSize, firstPage.airports().size());
        assertTrue(firstPage.hasMore());

        Set<String> airportCodes = new HashSet<>();
        for (int offset = 0; offset < firstPage.total(); offset += pageSize) {
            AirportSearchResponse page = airportSearchService.searchAirports(
                    "United States", offset, pageSize
            );
            page.airports().forEach(airport -> airportCodes.add(airport.iataCode()));
        }

        assertEquals(firstPage.total(), airportCodes.size());
    }
}
