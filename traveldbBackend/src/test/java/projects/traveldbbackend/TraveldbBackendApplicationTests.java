package projects.traveldbbackend;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.HashSet;
import java.util.Set;
import java.time.LocalDate;

import projects.traveldbbackend.rules.BaggageAdvice;
import projects.traveldbbackend.documents.DocumentRequirement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
class TraveldbBackendApplicationTests {

    @Autowired
    private TravelRepository repository;

    @Autowired
    private TravelService travelService;

    @Test
    void contextLoads() {
    }

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

    @Test
    void paginatesWithoutHidingCountrySearchMatches() {
        int pageSize = 100;
        TravelService.AirportSearchResponse firstPage = travelService.searchAirports(
                "United States", 0, pageSize
        );

        assertTrue(firstPage.total() > pageSize);
        assertEquals(pageSize, firstPage.airports().size());
        assertTrue(firstPage.hasMore());

        Set<String> airportCodes = new HashSet<>();
        for (int offset = 0; offset < firstPage.total(); offset += pageSize) {
            TravelService.AirportSearchResponse page = travelService.searchAirports(
                    "United States", offset, pageSize
            );
            page.airports().forEach(airport -> airportCodes.add(airport.iataCode()));
        }

        assertEquals(firstPage.total(), airportCodes.size());
    }

    @Test
    void listsEveryBaggagePickupInJourneyOrder() {
        TravelService.JourneyResponse response = travelService.checkJourney(
                new TravelService.JourneyRequest(
                        "NO",
                        List.of("OSL", "ATL", "LHR", "JFK", "OSL")
                )
        );

        assertTrue(response.pickupRequired());
        assertEquals(List.of("ATL", "JFK"), response.pickupAt());
    }

    @Test
    void requiresPickupAtSupportedInternationalToDomesticEntryPoints() {
        assertRequiredAt(List.of("OSL", "SYD", "MEL"), "SYD");
        assertRequiredAt(List.of("OSL", "AKL", "WLG"), "AKL");
        assertRequiredAt(List.of("OSL", "NRT", "CTS"), "NRT");
        assertRequiredAt(List.of("OSL", "DEL", "BOM"), "DEL");
    }

    @Test
    void appliesUsPreclearanceAndRemoteScreeningExceptionsConservatively() {
        TravelService.JourneyResponse precleared = travelService.checkJourney(
                request(List.of("DUB", "JFK", "BOS"), "SINGLE_BOOKING", "YES")
        );

        assertFalse(precleared.pickupRequired());
        assertEquals(BaggageAdvice.Status.NOT_REQUIRED, precleared.baggageStops().getFirst().status());
        assertTrue(precleared.baggageStops().getFirst().title().contains("Precleared"));

        TravelService.JourneyResponse pilotRoute = travelService.checkJourney(
                request(List.of("SYD", "LAX", "SFO"), "SINGLE_BOOKING", "YES")
        );
        assertEquals(BaggageAdvice.Status.REQUIRED, pilotRoute.baggageStops().getFirst().status());
        assertTrue(pilotRoute.baggageStops().getFirst().title().contains("screening pilot"));
    }

    @Test
    void separateTicketsRequireSelfTransferUnlessBagIsConfirmedThrough() {
        TravelService.JourneyResponse separateTickets = travelService.checkJourney(
                request(List.of("OSL", "LHR", "SIN"), "SEPARATE_TICKETS", "UNKNOWN")
        );
        assertEquals(List.of("LHR"), separateTickets.pickupAt());

        TravelService.JourneyResponse confirmedThrough = travelService.checkJourney(
                request(List.of("OSL", "LHR", "SIN"), "SEPARATE_TICKETS", "YES")
        );
        assertFalse(confirmedThrough.pickupRequired());
        assertEquals(BaggageAdvice.Status.NOT_REQUIRED, confirmedThrough.baggageStops().getFirst().status());
    }

    @Test
    void marksCanadaAndUnsupportedFirstEntryProcessesForConfirmation() {
        TravelService.JourneyResponse canada = travelService.checkJourney(
                request(List.of("OSL", "YYZ", "YVR"), "SINGLE_BOOKING", "UNKNOWN")
        );
        assertEquals(BaggageAdvice.Status.CONFIRM, canada.baggageStops().getFirst().status());

        TravelService.JourneyResponse unsupported = travelService.checkJourney(
                request(List.of("OSL", "GRU", "GIG"), "SINGLE_BOOKING", "YES")
        );
        assertEquals(BaggageAdvice.Status.CONFIRM, unsupported.baggageStops().getFirst().status());
    }

    @Test
    void skipsBaggageTransferStepsForCarryOnOnlyJourneys() {
        TravelService.JourneyResponse response = travelService.checkJourney(
                new TravelService.JourneyRequest(
                        "NO",
                        List.of("OSL", "ATL", "BOS"),
                        new TravelService.BaggageOptions(false, "UNKNOWN", "UNKNOWN")
                )
        );

        assertFalse(response.pickupRequired());
        assertTrue(response.baggageStops().isEmpty());
    }

    @Test
    void evaluatesDocumentsLocallyWithoutGuessingMissingCountryRules() {
        TravelService.JourneyResponse response = travelService.checkJourney(
                request(List.of("OSL", "DXB", "BKK"), "SINGLE_BOOKING", "YES")
        );

        assertEquals("TRAVELDB_LOCAL_RULES", response.documentCheck().provider());
        assertFalse(response.documentCheck().liveData());
        assertEquals("LOCAL_VERSIONED_RULE_SNAPSHOT", response.documentCheck().coverage());
        assertTrue(response.requiredDocuments().isEmpty());
        assertTrue(response.documentCheck().requirements().stream()
                .anyMatch(requirement -> requirement.scope() == DocumentRequirement.Scope.TRANSIT
                        && requirement.countryCode().equals("AE")
                        && requirement.status() == DocumentRequirement.Status.VERIFY));
        assertTrue(response.documentCheck().requirements().stream()
                .anyMatch(requirement -> requirement.scope() == DocumentRequirement.Scope.ENTRY
                        && requirement.countryCode().equals("TH")));
        assertTrue(response.documentCheck().verificationSources().stream()
                .allMatch(source -> source.sourceType().equals("GOVERNMENT")));
        assertTrue(response.documentCheck().warnings().stream()
                .anyMatch(warning -> warning.contains("no external requirements service was contacted")));
    }

    @Test
    void appliesOfficialLocalEtaRulesAndHeldVisaOverrides() {
        TravelService.JourneyResponse ukEta = travelService.checkJourney(
                documentRequest(List.of("OSL", "LHR"), "TOURISM", List.of())
        );
        assertTrue(ukEta.documentCheck().requirements().stream().anyMatch(requirement ->
                requirement.code().equals("UK_ETA_OR_VISA")
                        && requirement.status() == DocumentRequirement.Status.CONDITIONAL
                        && requirement.sources().stream().anyMatch(source -> source.url().contains("gov.uk"))
        ));

        TravelService.JourneyResponse usVisaHolder = travelService.checkJourney(
                documentRequest(List.of("OSL", "JFK"), "TOURISM", List.of("US"))
        );
        assertTrue(usVisaHolder.documentCheck().requirements().stream().anyMatch(requirement ->
                requirement.code().equals("ESTA")
                        && requirement.status() == DocumentRequirement.Status.NOT_REQUIRED
        ));
        assertFalse(usVisaHolder.documentCheck().requirements().stream().anyMatch(requirement ->
                requirement.code().equals("ESTA_OR_US_VISA")
        ));
    }

    @Test
    void evaluatesRepeatedTransitCountriesAsSeparateStops() {
        TravelService.JourneyResponse response = travelService.checkJourney(
                documentRequest(List.of("OSL", "YVR", "LHR", "YYZ", "BOS"), "TOURISM", List.of())
        );

        assertEquals(2, response.documentCheck().requirements().stream()
                .filter(requirement -> requirement.code().equals("CANADA_ETA_OR_VISA"))
                .count());
        assertTrue(response.documentCheck().requirements().stream()
                .anyMatch(requirement -> "YVR".equals(requirement.airportCode())));
        assertTrue(response.documentCheck().requirements().stream()
                .anyMatch(requirement -> "YYZ".equals(requirement.airportCode())));
    }

    @Test
    void reportsMissingDocumentInputsAndAcceptsACompleteTravelerProfile() {
        TravelService.JourneyResponse incomplete = travelService.checkJourney(
                request(List.of("OSL", "LHR"), "SINGLE_BOOKING", "YES")
        );
        assertTrue(incomplete.documentCheck().missingInputs().contains("Passport expiry date"));
        assertTrue(incomplete.documentCheck().missingInputs().contains("Departure date"));

        TravelService.JourneyResponse complete = travelService.checkJourney(
                new TravelService.JourneyRequest(
                        "NO",
                        List.of("OSL", "LHR"),
                        new TravelService.BaggageOptions(true, "SINGLE_BOOKING", "YES"),
                        new TravelService.DocumentOptions(
                                "NO",
                                "NO",
                                LocalDate.now().plusYears(3),
                                LocalDate.now().plusMonths(2),
                                "TOURISM",
                                30,
                                List.of(),
                                List.of()
                        )
                )
        );
        assertTrue(complete.documentCheck().missingInputs().isEmpty());
    }

    private void assertRequiredAt(List<String> route, String airportCode) {
        TravelService.JourneyResponse response = travelService.checkJourney(
                request(route, "SINGLE_BOOKING", "YES")
        );
        assertEquals(List.of(airportCode), response.pickupAt());
        assertEquals(BaggageAdvice.Status.REQUIRED, response.baggageStops().getFirst().status());
        assertFalse(response.baggageStops().getFirst().sources().isEmpty());
    }

    // Keep baggage-option setup explicit so each rule test documents its assumptions.
    private TravelService.JourneyRequest request(
            List<String> route,
            String ticketArrangement,
            String checkedThrough
    ) {
        return new TravelService.JourneyRequest(
                "NO",
                route,
                new TravelService.BaggageOptions(true, ticketArrangement, checkedThrough)
        );
    }

    private TravelService.JourneyRequest documentRequest(
            List<String> route,
            String purpose,
            List<String> heldVisas
    ) {
        return new TravelService.JourneyRequest(
                "NO",
                route,
                new TravelService.BaggageOptions(true, "SINGLE_BOOKING", "YES"),
                new TravelService.DocumentOptions(
                        "NO",
                        "NO",
                        LocalDate.of(2029, 5, 10),
                        LocalDate.of(2026, 9, 14),
                        purpose,
                        30,
                        List.of(),
                        heldVisas
                )
        );
    }

}
