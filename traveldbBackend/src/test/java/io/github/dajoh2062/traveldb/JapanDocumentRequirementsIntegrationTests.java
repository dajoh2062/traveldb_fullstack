package io.github.dajoh2062.traveldb;

import org.junit.jupiter.api.Test;
import io.github.dajoh2062.traveldb.api.dto.BaggageOptions;
import io.github.dajoh2062.traveldb.api.dto.DocumentOptions;
import io.github.dajoh2062.traveldb.api.dto.JourneyRequest;
import io.github.dajoh2062.traveldb.api.dto.JourneyResponse;
import io.github.dajoh2062.traveldb.api.dto.TravelDocument;
import io.github.dajoh2062.traveldb.documents.DocumentRequirement;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JapanDocumentRequirementsIntegrationTests extends DocumentRequirementsIntegrationTestSupport {

    @Test
    void nullPurposeJapanEntryFallsBackToGenericVerification() {
        JourneyResponse response = journeyService.checkJourney(
                new JourneyRequest("NO", List.of("OSL", "NRT"))
        );

        DocumentRequirement permission = requirement(response, "ENTRY_PERMISSION");
        assertEquals(DocumentRequirement.Status.VERIFY, permission.status());
        assertEquals("NRT", permission.airportCode());
        assertFalse(response.documentCheck().requirements().stream().anyMatch(requirement ->
                requirement.code().equals("JAPAN_SHORT_STAY_PERMISSION")
                        || requirement.code().equals("JAPAN_VISA_OR_SPECIAL_STATUS_CHECK")
        ));

        DocumentRequirement passport = requirement(response, "JAPAN_ENTRY_TRAVEL_DOCUMENT");
        assertEquals(DocumentRequirement.Status.REQUIRED, passport.status());
        assertEquals("Valid passport", factValue(passport, "Required at entry"));
        assertEquals("Valid through the stay", factValue(passport, "Recommended"));
        assertEquals("No additional margin stated by these sources",
                factValue(passport, "Post-trip margin"));
        assertTrue(passport.sources().stream().anyMatch(source ->
                source.url().contains("fi.emb-japan.go.jp")));
        assertTrue(hasEntryFallback(response, "NRT"));
        assertTrue(response.documentCheck().missingInputs().contains("Travel purpose"));
        assertEquals("2026-08-09.5", response.documentCheck().datasetVersion());
    }

    @Test
    void explicitTourismJapanEntryUsesTheReviewedWaiver() {
        JourneyResponse response = journeyService.checkJourney(
                documentRequest(List.of("OSL", "NRT"), "TOURISM", List.of())
        );

        DocumentRequirement permission = requirement(response, "JAPAN_SHORT_STAY_PERMISSION");
        assertEquals(DocumentRequirement.Status.NOT_REQUIRED, permission.status());
        assertEquals("japan-ordinary-passport-waiver-90", permission.ruleId());
        assertEquals("NRT", permission.airportCode());
        assertEquals("Up to 90 days", factValue(permission, "Landing period"));
        assertTrue(permission.sources().stream().anyMatch(source -> source.url().contains("mofa.go.jp")));
        assertFalse(hasEntryFallback(response, "NRT"));
    }

    @Test
    void preservesJapanPassportConditionsAndExactStayLimits() {
        DocumentRequirement thailand = requirement(
                journeyService.checkJourney(documentRequest(
                        "TH", List.of("OSL", "NRT"), "TOURISM", List.of())),
                "JAPAN_SHORT_STAY_PERMISSION"
        );
        assertEquals(DocumentRequirement.Status.CONDITIONAL, thailand.status());
        assertEquals("15 days", factValue(thailand, "Maximum stay"));
        assertEquals("ICAO-compliant ePassport required", factValue(thailand, "Passport"));

        DocumentRequirement qatar = requirement(
                journeyService.checkJourney(documentRequest(
                        "QA", List.of("OSL", "NRT"), "TOURISM", List.of())),
                "JAPAN_SHORT_STAY_PERMISSION"
        );
        assertEquals("30 consecutive days", factValue(qatar, "Maximum stay"));
        assertEquals("Required before departure", factValue(qatar, "Registration"));

        DocumentRequirement brazil = requirement(
                journeyService.checkJourney(documentRequest(
                        "BR", List.of("OSL", "NRT"), "TOURISM", List.of())),
                "JAPAN_SHORT_STAY_PERMISSION"
        );
        assertEquals("90 days", factValue(brazil, "Maximum stay"));
        assertEquals("ICAO-compliant ePassport required", factValue(brazil, "Passport"));

        DocumentRequirement austria = requirement(
                journeyService.checkJourney(documentRequest(
                        "AT", List.of("OSL", "NRT"), "TOURISM", List.of())),
                "JAPAN_SHORT_STAY_PERMISSION"
        );
        assertEquals(DocumentRequirement.Status.NOT_REQUIRED, austria.status());
        assertEquals("Up to 90 days", factValue(austria, "Initial landing period"));
        assertEquals("Up to 6 months after an approved extension",
                factValue(austria, "Bilateral maximum"));

        DocumentRequirement uruguay = requirement(
                journeyService.checkJourney(documentRequest(
                        "UY", List.of("OSL", "NRT"), "TOURISM", List.of())),
                "JAPAN_SHORT_STAY_PERMISSION"
        );
        assertEquals(DocumentRequirement.Status.CONDITIONAL, uruguay.status());
        assertEquals("Not recognized for travel",
                factValue(uruguay, "New version without place of birth"));
    }

    @Test
    void usesAConservativeJapanNonWaiverDecisionAndHeldVisaOverride() {
        JourneyResponse nonWaiver = journeyService.checkJourney(
                documentRequest("IN", List.of("OSL", "NRT"), "TOURISM", List.of())
        );
        DocumentRequirement visa = requirement(nonWaiver, "JAPAN_VISA_OR_SPECIAL_STATUS_CHECK");
        assertEquals(DocumentRequirement.Status.CONDITIONAL, visa.status());
        assertEquals("Visa before travel", factValue(visa, "Normal requirement"));
        assertTrue(visa.summary().contains("normally required before travel"));
        assertFalse(hasEntryFallback(nonWaiver, "NRT"));

        JourneyResponse visaHolder = journeyService.checkJourney(
                documentRequest(List.of("OSL", "NRT"), "TOURISM", List.of("JP"))
        );
        DocumentRequirement heldVisa = requirement(visaHolder, "JAPAN_VISA_VALIDITY");
        assertEquals(DocumentRequirement.Status.VERIFY, heldVisa.status());
        assertEquals("Japan visa recorded", factValue(heldVisa, "Profile input"));
        assertFalse(visaHolder.documentCheck().requirements().stream().anyMatch(candidate ->
                candidate.code().equals("JAPAN_SHORT_STAY_PERMISSION")
                        || candidate.code().equals("JAPAN_VISA_OR_SPECIAL_STATUS_CHECK")
        ));

        JourneyResponse japaneseCitizenWithRecordedVisa = journeyService.checkJourney(
                documentRequest("JP", List.of("OSL", "NRT"), "TOURISM", List.of("JP"))
        );
        assertEquals(DocumentRequirement.Status.NOT_REQUIRED,
                requirement(japaneseCitizenWithRecordedVisa, "JAPAN_CITIZEN_RETURN").status());
        assertFalse(japaneseCitizenWithRecordedVisa.documentCheck().requirements().stream().anyMatch(candidate ->
                candidate.code().equals("JAPAN_VISA_VALIDITY")
        ));
    }

    @Test
    void keepsJapanWorkAndStudyOnTheEntryFallback() {
        for (String purpose : List.of("WORK", "STUDY")) {
            JourneyResponse response = journeyService.checkJourney(
                    documentRequest(List.of("OSL", "NRT"), purpose, List.of())
            );

            assertTrue(hasEntryFallback(response, "NRT"));
            assertFalse(response.documentCheck().requirements().stream().anyMatch(requirement ->
                    requirement.code().startsWith("JAPAN_")
            ));
        }
    }

    @Test
    void aNonordinaryPrimaryDocumentDoesNotActivateJapanPassportRules() {
        JourneyResponse response = journeyService.checkJourney(new JourneyRequest(
                "NO",
                List.of("OSL", "NRT"),
                new BaggageOptions(true, "SINGLE_BOOKING", "YES"),
                new DocumentOptions(
                        "NO",
                        null,
                        null,
                        DEPARTURE_DATE,
                        "TOURISM",
                        30,
                        List.of(),
                        List.of(),
                        List.of(
                                new TravelDocument(
                                        "DIPLOMATIC_PASSPORT",
                                        null,
                                        "NO",
                                        PASSPORT_EXPIRY_DATE,
                                        true
                                ),
                                new TravelDocument(
                                        "PASSPORT",
                                        null,
                                        "NO",
                                        PASSPORT_EXPIRY_DATE,
                                        false
                                )
                        )
                )
        ));

        assertTrue(hasEntryFallback(response, "NRT"));
        assertFalse(response.documentCheck().requirements().stream().anyMatch(requirement ->
                requirement.code().startsWith("JAPAN_")
        ));
    }

    @Test
    void pureInternationalTransitThroughJapanRemainsOnTheTransitFallback() {
        JourneyResponse response = journeyService.checkJourney(
                request(List.of("OSL", "NRT", "SYD"), "SINGLE_BOOKING", "YES")
        );

        assertTrue(response.documentCheck().requirements().stream().anyMatch(requirement ->
                requirement.code().equals("TRANSIT_PERMISSION")
                        && requirement.status() == DocumentRequirement.Status.VERIFY
                        && requirement.scope() == DocumentRequirement.Scope.TRANSIT
                        && "JP".equals(requirement.countryCode())
                        && "NRT".equals(requirement.airportCode())
        ));
        assertFalse(response.documentCheck().requirements().stream().anyMatch(requirement ->
                requirement.code().startsWith("JAPAN_")
                        && "NRT".equals(requirement.airportCode())
        ));
    }
}
