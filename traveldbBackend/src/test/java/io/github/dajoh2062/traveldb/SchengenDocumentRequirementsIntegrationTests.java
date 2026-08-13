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

class SchengenDocumentRequirementsIntegrationTests extends DocumentRequirementsIntegrationTestSupport {

    @Test
    void keepsReviewedTravelDocumentsForIntraSchengenJourneys() {
        JourneyResponse response = journeyService.checkJourney(
                request(List.of("OSL", "CDG"), "SINGLE_BOOKING", "YES")
        );

        assertTrue(response.documentCheck().requirements().stream().anyMatch(requirement ->
                requirement.code().equals("EU_EEA_CH_TRAVEL_DOCUMENT")
                        && requirement.status() == DocumentRequirement.Status.REQUIRED
                        && "CDG".equals(requirement.airportCode())
        ));
        DocumentRequirement permission = requirement(response, "EU_EEA_CH_FREE_MOVEMENT");
        assertEquals(DocumentRequirement.Status.NOT_REQUIRED, permission.status());
        assertEquals("Not required", factValue(permission, "Visa"));
        assertEquals("Up to 3 months", factValue(permission, "Without residence registration"));
        assertFalse(response.documentActions().contains("EU_EEA_CH_FREE_MOVEMENT"));
    }

    @Test
    void appliesGroupedSchengenEntryRulesBeyondTheOriginalFiveCountries() {
        JourneyResponse freeMovement = journeyService.checkJourney(
                new JourneyRequest("NO", List.of("OSL", "ATH"))
        );
        DocumentRequirement freeMovementPermission = requirement(
                freeMovement,
                "EU_EEA_CH_FREE_MOVEMENT"
        );
        assertEquals(DocumentRequirement.Status.NOT_REQUIRED, freeMovementPermission.status());
        assertEquals("GR", freeMovementPermission.countryCode());
        assertEquals("ATH", freeMovementPermission.airportCode());
        assertFalse(hasEntryFallback(freeMovement, "ATH"));

        JourneyResponse visaExempt = journeyService.checkJourney(
                new JourneyRequest("US", List.of("JFK", "WAW"))
        );
        DocumentRequirement exemption = requirement(
                visaExempt,
                "SCHENGEN_SHORT_STAY_VISA_EXEMPT"
        );
        assertEquals(DocumentRequirement.Status.NOT_REQUIRED, exemption.status());
        assertEquals("Up to 90 days in any 180-day period across Schengen",
                factValue(exemption, "Maximum stay"));
        assertEquals("At least 3 months after planned departure from Schengen",
                factValue(exemption, "Passport validity"));
        assertEquals("Not required through 30 September 2026",
                factValue(exemption, "Electronic authorisation"));
        assertFalse(hasEntryFallback(visaExempt, "WAW"));

        JourneyResponse visaRequired = journeyService.checkJourney(
                new JourneyRequest("IN", List.of("DEL", "VIE"))
        );
        DocumentRequirement visa = requirement(
                visaRequired,
                "SCHENGEN_SHORT_STAY_VISA_REQUIRED"
        );
        assertEquals(DocumentRequirement.Status.CONDITIONAL, visa.status());
        assertTrue(visa.title().contains("unless exempt"));
        assertEquals("Required before travel unless an exemption applies", factValue(visa, "Visa"));
        assertFalse(hasEntryFallback(visaRequired, "VIE"));
    }

    @Test
    void doesNotOverclaimForAnnexIPassportsWithRecordedSchengenPermission() {
        JourneyResponse response = journeyService.checkJourney(new JourneyRequest(
                "IN",
                List.of("DEL", "VIE"),
                new BaggageOptions(true, "SINGLE_BOOKING", "YES"),
                new DocumentOptions(
                        "IN",
                        "IN",
                        PASSPORT_EXPIRY_DATE,
                        DEPARTURE_DATE,
                        "TOURISM",
                        30,
                        List.of("FR"),
                        List.of("FR")
                )
        ));

        DocumentRequirement requirement = requirement(
                response,
                "SCHENGEN_SHORT_STAY_VISA_REQUIRED"
        );
        assertEquals(DocumentRequirement.Status.CONDITIONAL, requirement.status());
        assertTrue(requirement.conditions().stream().anyMatch(condition ->
                condition.contains("residence permit") && condition.contains("exemption")
        ));
    }

    @Test
    void keepsQualifiedAnnexIIAndFutureEtiasCasesConservative() {
        JourneyResponse biometric = journeyService.checkJourney(
                new JourneyRequest("UA", List.of("KBP", "WAW"))
        );
        assertEquals(DocumentRequirement.Status.CONDITIONAL,
                requirement(biometric, "SCHENGEN_BIOMETRIC_PASSPORT_CHECK").status());

        JourneyResponse futureDeparture = journeyService.checkJourney(new JourneyRequest(
                "US",
                List.of("JFK", "WAW"),
                new BaggageOptions(true, "SINGLE_BOOKING", "YES"),
                new DocumentOptions(
                        "US",
                        "US",
                        LocalDate.of(2029, 1, 1),
                        LocalDate.of(2026, 10, 1),
                        "TOURISM",
                        30,
                        List.of(),
                        List.of()
                )
        ));
        assertTrue(hasEntryFallback(futureDeparture, "WAW"));
        assertFalse(futureDeparture.documentCheck().requirements().stream().anyMatch(candidate ->
                candidate.code().equals("SCHENGEN_SHORT_STAY_VISA_EXEMPT")
        ));
    }
}
