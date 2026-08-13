package io.github.dajoh2062.traveldb;

import org.junit.jupiter.api.Test;
import io.github.dajoh2062.traveldb.api.dto.BaggageOptions;
import io.github.dajoh2062.traveldb.api.dto.DocumentOptions;
import io.github.dajoh2062.traveldb.api.dto.JourneyRequest;
import io.github.dajoh2062.traveldb.api.dto.JourneyResponse;
import io.github.dajoh2062.traveldb.documents.DocumentRequirement;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UnitedStatesDocumentRequirementsIntegrationTests extends DocumentRequirementsIntegrationTestSupport {

    @Test
    void basicUsEntryStillReturnsTheEstaOrVisaRequirement() {
        JourneyResponse response = journeyService.checkJourney(
                request(List.of("OSL", "JFK"), "SINGLE_BOOKING", "YES")
        );

        DocumentRequirement requirement = requirement(response, "ESTA_OR_US_VISA");

        assertEquals(DocumentRequirement.Status.CONDITIONAL, requirement.status());
        assertEquals(DocumentRequirement.Scope.ENTRY, requirement.scope());
        assertEquals("JFK", requirement.airportCode());
        assertEquals("Up to 90 days per admission", factValue(requirement, "Maximum stay"));
        assertEquals("Usually 2 years or until the passport expires",
                factValue(requirement, "ESTA validity"));
        assertEquals("2026-08-09.5", response.documentCheck().datasetVersion());
    }

    @Test
    void appliesUsEstaRulesToPuertoRicoAndTheUsVirginIslands() {
        JourneyResponse puertoRico = journeyService.checkJourney(
                request(List.of("OSL", "SJU"), "SINGLE_BOOKING", "YES")
        );
        assertTrue(puertoRico.documentCheck().requirements().stream().anyMatch(requirement ->
                requirement.code().equals("ESTA_OR_US_VISA")
                        && "PR".equals(requirement.countryCode())
                        && "SJU".equals(requirement.airportCode())
        ));

        JourneyResponse virginIslands = journeyService.checkJourney(
                request(List.of("OSL", "STT"), "SINGLE_BOOKING", "YES")
        );
        assertTrue(virginIslands.documentCheck().requirements().stream().anyMatch(requirement ->
                requirement.code().equals("ESTA_OR_US_VISA")
                        && "VI".equals(requirement.countryCode())
                        && "STT".equals(requirement.airportCode())
        ));
    }

    @Test
    void selectedPassportCountryDrivesElectronicAuthorizationEligibility() {
        JourneyResponse response = journeyService.checkJourney(
                new JourneyRequest(
                        "NO",
                        List.of("OSL", "JFK"),
                        new BaggageOptions(true, "SINGLE_BOOKING", "YES"),
                        new DocumentOptions(
                                "NO",
                                "BR",
                                PASSPORT_EXPIRY_DATE,
                                DEPARTURE_DATE,
                                "TOURISM",
                                30,
                                List.of(),
                                List.of()
                        )
                )
        );

        assertTrue(response.documentCheck().requirements().stream().anyMatch(requirement ->
                requirement.code().equals("ENTRY_PERMISSION")
                        && requirement.status() == DocumentRequirement.Status.VERIFY
                        && "JFK".equals(requirement.airportCode())
        ));
        assertFalse(response.documentCheck().requirements().stream().anyMatch(requirement ->
                requirement.code().equals("ESTA_OR_US_VISA")
        ));
    }

    @Test
    void workAndStudyPurposesDoNotReuseVisitorAuthorizationRules() {
        JourneyResponse response = journeyService.checkJourney(
                documentRequest(List.of("OSL", "JFK"), "WORK", List.of())
        );

        assertTrue(response.documentCheck().requirements().stream().anyMatch(requirement ->
                requirement.code().equals("ENTRY_PERMISSION")
                        && requirement.status() == DocumentRequirement.Status.VERIFY
                        && "JFK".equals(requirement.airportCode())
        ));
        assertFalse(response.documentCheck().requirements().stream().anyMatch(requirement ->
                requirement.code().equals("ESTA_OR_US_VISA")
        ));
    }
}
