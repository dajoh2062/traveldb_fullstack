package io.github.dajoh2062.traveldb;

import org.junit.jupiter.api.Test;
import io.github.dajoh2062.traveldb.api.dto.BaggageOptions;
import io.github.dajoh2062.traveldb.api.dto.DocumentOptions;
import io.github.dajoh2062.traveldb.api.dto.JourneyRequest;
import io.github.dajoh2062.traveldb.api.dto.JourneyResponse;
import io.github.dajoh2062.traveldb.documents.DocumentRequirement;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GeneralDocumentRequirementsIntegrationTests extends DocumentRequirementsIntegrationTestSupport {

    @Test
    void evaluatesDocumentsLocallyWithoutGuessingMissingCountryRules() {
        JourneyResponse response = journeyService.checkJourney(
                request(List.of("OSL", "DXB", "BKK"), "SINGLE_BOOKING", "YES")
        );

        assertEquals("TRAVELDB_LOCAL_RULES", response.documentCheck().provider());
        assertFalse(response.documentCheck().liveData());
        assertEquals("LOCAL_RULES_WITH_VERIFY_FALLBACK", response.documentCheck().coverage());
        assertTrue(response.documentActions().contains("TRANSIT_PERMISSION"));
        assertTrue(response.documentActions().contains("ENTRY_PERMISSION"));
        assertTrue(response.documentCheck().requirements().stream()
                .anyMatch(requirement -> requirement.scope() == DocumentRequirement.Scope.TRANSIT
                        && requirement.countryCode().equals("AE")
                        && requirement.status() == DocumentRequirement.Status.VERIFY
                        && "conservative-transit-permission".equals(requirement.ruleId())));
        assertTrue(response.documentCheck().requirements().stream()
                .anyMatch(requirement -> requirement.scope() == DocumentRequirement.Scope.ENTRY
                        && requirement.countryCode().equals("TH")
                        && "conservative-entry-permission".equals(requirement.ruleId())));
        assertTrue(response.documentCheck().verificationSources().stream()
                .anyMatch(source -> source.url().contains("iatatravelcentre.com")));
        assertFalse(response.documentCheck().verificationSources().stream()
                .anyMatch(source -> source.url().contains("gov.uk")));
        assertTrue(response.documentCheck().warnings().stream()
                .anyMatch(warning -> warning.contains("no external requirements service was contacted")));
    }

    @Test
    void placesUsAndAustralianAuthorizationsAtTheBorderAirports() {
        JourneyResponse response = journeyService.checkJourney(
                request(List.of("OSL", "JFK", "BNE", "MEL"), "SINGLE_BOOKING", "YES")
        );

        assertTrue(response.documentCheck().requirements().stream().anyMatch(requirement ->
                requirement.code().equals("ESTA_OR_US_VISA")
                        && requirement.status() == DocumentRequirement.Status.CONDITIONAL
                        && requirement.scope() == DocumentRequirement.Scope.ENTRY
                        && "US".equals(requirement.countryCode())
                        && "JFK".equals(requirement.airportCode())
        ));
        assertTrue(response.documentCheck().requirements().stream().anyMatch(requirement ->
                requirement.code().equals("AUSTRALIA_VISITOR_VISA")
                        && requirement.status() == DocumentRequirement.Status.CONDITIONAL
                        && requirement.scope() == DocumentRequirement.Scope.ENTRY
                        && "AU".equals(requirement.countryCode())
                        && "BNE".equals(requirement.airportCode())
        ));
        assertFalse(response.documentCheck().requirements().stream().anyMatch(requirement ->
                "AU_TRANSIT_VISA".equals(requirement.code()) && "BNE".equals(requirement.airportCode())
        ));
        assertFalse(response.documentCheck().requirements().stream().anyMatch(requirement ->
                requirement.scope() == DocumentRequirement.Scope.ENTRY
                        && "MEL".equals(requirement.airportCode())
        ));
        assertTrue(response.documentActions().contains("ESTA_OR_US_VISA"));
        assertTrue(response.documentActions().contains("AUSTRALIA_VISITOR_VISA"));
    }

    @Test
    void appliesOfficialLocalEtaRulesAndHeldVisaOverrides() {
        JourneyResponse ukEta = journeyService.checkJourney(
                documentRequest(List.of("OSL", "LHR"), "TOURISM", List.of())
        );
        DocumentRequirement ukRequirement = requirement(ukEta, "UK_ETA_OR_VISA");
        assertEquals(DocumentRequirement.Status.CONDITIONAL, ukRequirement.status());
        assertTrue(ukRequirement.sources().stream().anyMatch(source -> source.url().contains("gov.uk")));
        assertEquals("Up to 6 months per visit", factValue(ukRequirement, "Maximum stay"));
        assertEquals("2 years or until the passport expires", factValue(ukRequirement, "ETA validity"));
        assertEquals("Valid for the whole stay", factValue(ukRequirement, "Passport validity"));

        JourneyResponse usVisaHolder = journeyService.checkJourney(
                documentRequest(List.of("OSL", "JFK"), "TOURISM", List.of("US"))
        );
        DocumentRequirement usVisa = requirement(usVisaHolder, "US_VISA_VALIDITY");
        assertEquals(DocumentRequirement.Status.VERIFY, usVisa.status());
        assertEquals("U.S. visa recorded", factValue(usVisa, "Profile input"));
        assertFalse(usVisaHolder.documentCheck().requirements().stream().anyMatch(requirement ->
                requirement.code().equals("ESTA_OR_US_VISA")
        ));
    }

    @Test
    void evaluatesRepeatedTransitCountriesAsSeparateStops() {
        JourneyResponse response = journeyService.checkJourney(
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
        JourneyResponse incomplete = journeyService.checkJourney(
                request(List.of("OSL", "LHR"), "SINGLE_BOOKING", "YES")
        );
        assertTrue(incomplete.documentCheck().missingInputs().contains("Passport expiry date"));
        assertTrue(incomplete.documentCheck().missingInputs().contains("Departure date"));

        JourneyResponse complete = journeyService.checkJourney(
                new JourneyRequest(
                        "NO",
                        List.of("OSL", "LHR"),
                        new BaggageOptions(true, "SINGLE_BOOKING", "YES"),
                        new DocumentOptions(
                                "NO",
                                "NO",
                                LocalDate.now(TEST_CLOCK).plusYears(3),
                                LocalDate.now(TEST_CLOCK).plusMonths(2),
                                "TOURISM",
                                30,
                                List.of(),
                                List.of()
                        )
                )
        );
        assertTrue(complete.documentCheck().missingInputs().isEmpty());
    }
}
