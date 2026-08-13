package io.github.dajoh2062.traveldb;

import org.junit.jupiter.api.Test;
import io.github.dajoh2062.traveldb.api.dto.JourneyRequest;
import io.github.dajoh2062.traveldb.api.dto.JourneyResponse;
import io.github.dajoh2062.traveldb.documents.DocumentRequirement;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CountrySpecificDocumentRequirementsIntegrationTests extends DocumentRequirementsIntegrationTestSupport {

    @Test
    void returnsTheAustralianEtaPathForAnEligibleUsPassport() {
        JourneyResponse response = journeyService.checkJourney(
                new JourneyRequest("US", List.of("LAX", "SYD"))
        );

        DocumentRequirement requirement = requirement(response, "AUSTRALIA_VISITOR_VISA");

        assertEquals("Australian ETA or another suitable visa", requirement.title());
        assertEquals(DocumentRequirement.Status.CONDITIONAL, requirement.status());
        assertEquals("SYD", requirement.airportCode());
        assertEquals("Up to 3 months per entry", factValue(requirement, "Maximum stay"));
        assertEquals("A visa is required before travel", factValue(requirement, "Visa status"));
    }

    @Test
    void requiresAustralianEntryPermissionBeforeAnIntermediateDomesticLeg() {
        JourneyResponse response = journeyService.checkJourney(
                request(List.of("OSL", "SYD", "MEL", "AKL"), "SINGLE_BOOKING", "YES")
        );

        assertTrue(response.documentCheck().requirements().stream().anyMatch(requirement ->
                requirement.code().equals("AUSTRALIA_VISITOR_VISA")
                        && requirement.scope() == DocumentRequirement.Scope.ENTRY
                        && "SYD".equals(requirement.airportCode())
        ));
        assertFalse(response.documentCheck().requirements().stream().anyMatch(requirement ->
                requirement.code().equals("AU_TRANSIT_VISA")
                        && "SYD".equals(requirement.airportCode())
        ));
    }

    @Test
    void baggageCollectionAtAnAustralianTransitStopRequiresEntryPermission() {
        JourneyResponse response = journeyService.checkJourney(
                request(List.of("OSL", "SYD", "AKL"), "SEPARATE_TICKETS", "NO")
        );

        assertTrue(response.pickupAt().contains("SYD"));
        assertTrue(response.documentCheck().requirements().stream().anyMatch(requirement ->
                requirement.code().equals("AUSTRALIA_VISITOR_VISA")
                        && requirement.scope() == DocumentRequirement.Scope.ENTRY
                        && "SYD".equals(requirement.airportCode())
        ));
        assertFalse(response.documentCheck().requirements().stream().anyMatch(requirement ->
                requirement.code().equals("AU_TRANSIT_VISA")
                        && "SYD".equals(requirement.airportCode())
        ));
    }

    @Test
    void appliesTheReviewedBrazilVisaWaiverAtTheFirstArrivalAirport() {
        JourneyResponse response = journeyService.checkJourney(
                request(List.of("OSL", "GRU", "GIG"), "SINGLE_BOOKING", "YES")
        );

        DocumentRequirement requirement = requirement(response, "BRAZIL_VISA_EXEMPT");

        assertEquals(DocumentRequirement.Status.NOT_REQUIRED, requirement.status());
        assertEquals(DocumentRequirement.Scope.ENTRY, requirement.scope());
        assertEquals("BR", requirement.countryCode());
        assertEquals("GRU", requirement.airportCode());
        assertEquals("Not required for tourism", factValue(requirement, "Visa or ETA"));
        assertEquals("Up to 90 days from first entry", factValue(requirement, "Initial stay"));
        assertEquals("Through the entire stay; no extra 6 months required",
                factValue(requirement, "Passport validity"));
        assertTrue(requirement.sources().stream().anyMatch(source -> source.url().contains("gov.br")));
        assertTrue(requirement.sources().stream().anyMatch(source -> source.url().contains("regjeringen.no")));
        assertFalse(response.documentActions().contains("BRAZIL_VISA_EXEMPT"));
        assertFalse(response.documentCheck().requirements().stream().anyMatch(candidate ->
                candidate.code().equals("ENTRY_PERMISSION") && "GRU".equals(candidate.airportCode())
        ));
        assertFalse(response.documentCheck().requirements().stream().anyMatch(candidate ->
                candidate.scope() == DocumentRequirement.Scope.ENTRY
                        && "GIG".equals(candidate.airportCode())
        ));
    }

    @Test
    void explicitTourismUsesTheBrazilVisaWaiver() {
        JourneyResponse response = journeyService.checkJourney(
                documentRequest(List.of("OSL", "GIG"), "TOURISM", List.of())
        );

        DocumentRequirement requirement = requirement(response, "BRAZIL_VISA_EXEMPT");

        assertEquals(DocumentRequirement.Status.NOT_REQUIRED, requirement.status());
        assertEquals("GIG", requirement.airportCode());
        assertFalse(response.documentActions().contains("BRAZIL_VISA_EXEMPT"));
    }

    @Test
    void keepsBrazilBusinessTravelOnTheVerificationPath() {
        JourneyResponse response = journeyService.checkJourney(
                documentRequest(List.of("OSL", "GIG"), "BUSINESS", List.of())
        );

        DocumentRequirement requirement = requirement(response, "BRAZIL_BUSINESS_PERMISSION");

        assertEquals(DocumentRequirement.Status.VERIFY, requirement.status());
        assertEquals("GIG", requirement.airportCode());
        assertTrue(response.documentActions().contains("BRAZIL_BUSINESS_PERMISSION"));
        assertFalse(response.documentCheck().requirements().stream().anyMatch(candidate ->
                candidate.code().equals("BRAZIL_VISA_EXEMPT")
        ));
    }

    @Test
    void unsupportedPassportsStillUseTheBrazilEntryFallback() {
        JourneyResponse response = journeyService.checkJourney(
                new JourneyRequest("IN", List.of("DEL", "GIG"))
        );

        assertTrue(response.documentCheck().requirements().stream().anyMatch(requirement ->
                requirement.code().equals("ENTRY_PERMISSION")
                        && requirement.status() == DocumentRequirement.Status.VERIFY
                        && "BR".equals(requirement.countryCode())
                        && "GIG".equals(requirement.airportCode())
        ));
        assertFalse(response.documentCheck().requirements().stream().anyMatch(requirement ->
                requirement.code().equals("BRAZIL_VISA_EXEMPT")
        ));
    }

    @Test
    void distinguishesAustralianElectronicVisaEligibilityBoundaries() {
        JourneyResponse bulgarian = journeyService.checkJourney(
                new JourneyRequest("BG", List.of("OSL", "SYD"))
        );
        assertTrue(bulgarian.documentCheck().requirements().stream().anyMatch(requirement ->
                requirement.title().equals("Australian eVisitor or another suitable visa")
                        && "SYD".equals(requirement.airportCode())
        ));

        JourneyResponse vatican = journeyService.checkJourney(
                new JourneyRequest("VA", List.of("OSL", "SYD"))
        );
        assertTrue(vatican.documentCheck().requirements().stream().anyMatch(requirement ->
                requirement.title().equals("Australian visa — eVisitor or ETA may be available")
                        && "SYD".equals(requirement.airportCode())
        ));

        JourneyResponse indian = journeyService.checkJourney(
                new JourneyRequest("IN", List.of("OSL", "SYD"))
        );
        assertTrue(indian.documentCheck().requirements().stream().anyMatch(requirement ->
                requirement.code().equals("ENTRY_PERMISSION")
                        && requirement.status() == DocumentRequirement.Status.VERIFY
                        && "SYD".equals(requirement.airportCode())
        ));
    }

    @Test
    void returnsSpecificCanadianAndNewZealandStayLimits() {
        DocumentRequirement canada = requirement(
                journeyService.checkJourney(new JourneyRequest("NO", List.of("OSL", "YYZ"))),
                "CANADA_ETA_OR_VISA"
        );
        assertEquals("Normally up to 6 months per visit", factValue(canada, "Maximum stay"));
        assertEquals("Up to 5 years or until the passport expires", factValue(canada, "eTA validity"));

        DocumentRequirement newZealand = requirement(
                journeyService.checkJourney(new JourneyRequest("NO", List.of("OSL", "AKL"))),
                "NZETA"
        );
        assertEquals("Up to 3 months per visit", factValue(newZealand, "Maximum stay"));
        assertEquals("Up to 6 months total in any 12-month period",
                factValue(newZealand, "12-month limit"));
        assertEquals("2 years for most travellers", factValue(newZealand, "NZeTA validity"));
        assertEquals("Normally at least 3 months after planned departure",
                factValue(newZealand, "Passport validity"));
    }

    @Test
    void coversAuditedTaiwanEntryAndBoliviaTransitRules() {
        JourneyResponse taiwanToUk = journeyService.checkJourney(
                new JourneyRequest("TW", List.of("TPE", "LHR"))
        );
        assertTrue(taiwanToUk.documentCheck().requirements().stream().anyMatch(requirement ->
                requirement.code().equals("UK_ETA_OR_VISA")
                        && "LHR".equals(requirement.airportCode())
        ));

        JourneyResponse boliviaViaAuckland = journeyService.checkJourney(
                new JourneyRequest("BO", List.of("OSL", "AKL", "SYD"))
        );
        assertTrue(boliviaViaAuckland.documentCheck().requirements().stream().anyMatch(requirement ->
                requirement.code().equals("NZETA_OR_TRANSIT_VISA")
                        && "AKL".equals(requirement.airportCode())
        ));
    }

    @Test
    void keepsCanadianEtaPassportQualifiersVisible() {
        JourneyResponse romanianEntry = journeyService.checkJourney(
                new JourneyRequest("RO", List.of("OTP", "YYZ"))
        );
        assertTrue(romanianEntry.documentCheck().requirements().stream().anyMatch(requirement ->
                requirement.code().equals("CANADA_ETA_OR_VISA")
                        && requirement.scope() == DocumentRequirement.Scope.ENTRY
                        && requirement.summary().contains("electronic passport")
        ));

        JourneyResponse romanianTransit = journeyService.checkJourney(
                new JourneyRequest("RO", List.of("OTP", "YYZ", "JFK"))
        );
        assertTrue(romanianTransit.documentCheck().requirements().stream().anyMatch(requirement ->
                requirement.code().equals("CANADA_ETA_OR_VISA")
                        && requirement.scope() == DocumentRequirement.Scope.TRANSIT
                        && requirement.summary().contains("non-electronic passports")
        ));

        JourneyResponse taiwanEntry = journeyService.checkJourney(
                new JourneyRequest("TW", List.of("TPE", "YYZ"))
        );
        assertTrue(taiwanEntry.documentCheck().requirements().stream().anyMatch(requirement ->
                requirement.code().equals("CANADA_ETA_OR_VISA")
                        && requirement.scope() == DocumentRequirement.Scope.ENTRY
                        && requirement.summary().contains("personal identification number")
        ));

        JourneyResponse taiwanTransit = journeyService.checkJourney(
                new JourneyRequest("TW", List.of("TPE", "YVR", "LAX"))
        );
        assertTrue(taiwanTransit.documentCheck().requirements().stream().anyMatch(requirement ->
                requirement.code().equals("CANADA_ETA_OR_VISA")
                        && requirement.scope() == DocumentRequirement.Scope.TRANSIT
                        && requirement.summary().contains("personal identification number")
        ));
    }
}
