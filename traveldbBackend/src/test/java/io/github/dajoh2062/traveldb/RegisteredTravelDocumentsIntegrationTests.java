package io.github.dajoh2062.traveldb;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import io.github.dajoh2062.traveldb.api.dto.BaggageOptions;
import io.github.dajoh2062.traveldb.api.dto.DocumentOptions;
import io.github.dajoh2062.traveldb.api.dto.JourneyRequest;
import io.github.dajoh2062.traveldb.api.dto.JourneyResponse;
import io.github.dajoh2062.traveldb.api.dto.TravelDocument;
import io.github.dajoh2062.traveldb.api.dto.TravelDocument.Type;
import io.github.dajoh2062.traveldb.documents.DocumentRequirement;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RegisteredTravelDocumentsIntegrationTests extends DocumentRequirementsIntegrationTestSupport {

    @Test
    void registersMultipleDocumentsAndUsesARegisteredVisaInExistingRules() {
        JourneyResponse response = journeyService.checkJourney(new JourneyRequest(
                "NO",
                List.of("OSL", "JFK"),
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
                                        "PASSPORT",
                                        null,
                                        "NO",
                                        PASSPORT_EXPIRY_DATE,
                                        true
                                ),
                                new TravelDocument(
                                        "VISA",
                                        null,
                                        "US",
                                        PASSPORT_EXPIRY_DATE,
                                        false
                                ),
                                new TravelDocument(
                                        "SEAFARER_IDENTITY_DOCUMENT",
                                        null,
                                        "NO",
                                        PASSPORT_EXPIRY_DATE,
                                        false
                                )
                        )
                )
        ));

        assertTrue(response.documentCheck().requirements().stream().anyMatch(requirement ->
                requirement.code().equals("US_VISA_VALIDITY")
        ));
        assertEquals(3, response.documentCheck().requirements().stream()
                .filter(requirement -> requirement.code().startsWith("DOCUMENT_ACCEPTANCE_"))
                .count());
        assertTrue(response.documentCheck().requirements().stream().anyMatch(requirement ->
                requirement.title().contains("seafarer")
                        && requirement.status() == DocumentRequirement.Status.VERIFY
                        && requirement.sources().stream()
                        .anyMatch(source -> source.url().contains("ilo.org"))
        ));
    }

    @Test
    void anAlternativePrimaryDocumentKeepsPassportWaiverRulesVerificationOnly() {
        JourneyResponse response = journeyService.checkJourney(new JourneyRequest(
                "NO",
                List.of("OSL", "JFK"),
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
                                        "REFUGEE_TRAVEL_DOCUMENT",
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

        assertTrue(response.documentCheck().requirements().stream().anyMatch(requirement ->
                requirement.code().equals("ENTRY_PERMISSION")
                        && requirement.status() == DocumentRequirement.Status.VERIFY
        ));
        assertFalse(response.documentCheck().requirements().stream().anyMatch(requirement ->
                requirement.code().equals("ESTA_OR_US_VISA")
        ));
        assertTrue(response.documentCheck().requirements().stream().anyMatch(requirement ->
                requirement.title().contains("refugee")
                        && requirement.sources().stream()
                        .anyMatch(source -> source.url().contains("unhcr.org"))
        ));
    }

    @ParameterizedTest
    @EnumSource(value = Type.class, names = {
            "DIPLOMATIC_PASSPORT",
            "SERVICE_PASSPORT",
            "OFFICIAL_PASSPORT",
            "MILITARY_PASSPORT"
    })
    void aSpecialPassportDoesNotActivateOrdinaryPassportWaivers(Type specialPassportType) {
        JourneyResponse response = journeyService.checkJourney(new JourneyRequest(
                "NO",
                List.of("OSL", "JFK"),
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
                                        specialPassportType.name(),
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

        assertFalse(response.documentCheck().requirements().stream().anyMatch(requirement ->
                requirement.code().equals("ESTA_OR_US_VISA")
        ));
        assertTrue(response.documentCheck().requirements().stream().anyMatch(requirement ->
                requirement.code().equals("ENTRY_PERMISSION")
                        && requirement.status() == DocumentRequirement.Status.VERIFY
        ));
        assertTrue(response.documentCheck().requirements().stream().anyMatch(requirement ->
                requirement.code().startsWith("DOCUMENT_ACCEPTANCE_")
                        && requirement.title().contains(specialPassportType.displayName())
                        && requirement.status() == DocumentRequirement.Status.VERIFY
        ));
    }

    @Test
    void anOrdinaryPrimaryPassportStillActivatesWaiversWithOtherPassportsRegistered() {
        JourneyResponse response = journeyService.checkJourney(new JourneyRequest(
                "NO",
                List.of("OSL", "JFK"),
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
                                        "PASSPORT",
                                        null,
                                        "NO",
                                        PASSPORT_EXPIRY_DATE,
                                        true
                                ),
                                new TravelDocument(
                                        "DIPLOMATIC_PASSPORT",
                                        null,
                                        "SE",
                                        PASSPORT_EXPIRY_DATE,
                                        false
                                ),
                                new TravelDocument(
                                        "PASSPORT",
                                        null,
                                        "BR",
                                        PASSPORT_EXPIRY_DATE,
                                        false
                                )
                        )
                )
        ));

        assertTrue(response.documentCheck().requirements().stream().anyMatch(requirement ->
                requirement.code().equals("ESTA_OR_US_VISA")
                        && requirement.status() == DocumentRequirement.Status.CONDITIONAL
        ));
    }

    @Test
    void thePrimaryPassportFromTheNewListDrivesNationalityRules() {
        JourneyResponse response = journeyService.checkJourney(new JourneyRequest(
                "NO",
                List.of("OSL", "JFK"),
                new BaggageOptions(true, "SINGLE_BOOKING", "YES"),
                new DocumentOptions(
                        "NO",
                        "NO",
                        PASSPORT_EXPIRY_DATE,
                        DEPARTURE_DATE,
                        "TOURISM",
                        30,
                        List.of(),
                        List.of(),
                        List.of(
                                new TravelDocument(
                                        "PASSPORT",
                                        null,
                                        "NO",
                                        PASSPORT_EXPIRY_DATE,
                                        false
                                ),
                                new TravelDocument(
                                        "PASSPORT",
                                        null,
                                        "BR",
                                        PASSPORT_EXPIRY_DATE,
                                        true
                                )
                        )
                )
        ));

        assertTrue(response.documentCheck().requirements().stream().anyMatch(requirement ->
                requirement.code().equals("ENTRY_PERMISSION")
                        && requirement.status() == DocumentRequirement.Status.VERIFY
        ));
        assertFalse(response.documentCheck().requirements().stream().anyMatch(requirement ->
                requirement.code().equals("ESTA_OR_US_VISA")
        ));
    }

    @Test
    void aNationalIdentityCardCanUseReviewedRegionalTravelDocumentRules() {
        JourneyResponse response = journeyService.checkJourney(new JourneyRequest(
                "NO",
                List.of("OSL", "CDG"),
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
                        List.of(new TravelDocument(
                                "NATIONAL_ID_CARD",
                                null,
                                "NO",
                                PASSPORT_EXPIRY_DATE,
                                true
                        ))
                )
        ));

        assertTrue(response.documentCheck().requirements().stream().anyMatch(requirement ->
                requirement.code().equals("EU_EEA_CH_TRAVEL_DOCUMENT")
                        && requirement.status() == DocumentRequirement.Status.REQUIRED
                        && "CDG".equals(requirement.airportCode())
        ));
        assertEquals(DocumentRequirement.Status.NOT_REQUIRED,
                requirement(response, "EU_EEA_CH_FREE_MOVEMENT").status());
        assertFalse(response.documentCheck().requirements().stream().anyMatch(requirement ->
                requirement.code().equals("ENTRY_PERMISSION")
                        && "CDG".equals(requirement.airportCode())
        ));
    }

    @Test
    void aNationalIdentityCardDoesNotActivatePassportWaiversOutsideReviewedRegionalRules() {
        JourneyResponse response = journeyService.checkJourney(new JourneyRequest(
                "NO",
                List.of("OSL", "JFK"),
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
                        List.of(new TravelDocument(
                                "NATIONAL_ID_CARD",
                                null,
                                "NO",
                                PASSPORT_EXPIRY_DATE,
                                true
                        ))
                )
        ));

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
    void acceptsEverySupportedDocumentTypeInOneTravelerProfile() {
        List<TravelDocument> documents = java.util.Arrays.stream(Type.values())
                .map(type -> new TravelDocument(
                        type.name(),
                        type == Type.OTHER ? "Border crossing card" : null,
                        type.issuingCountryRequired() ? "NO" : null,
                        PASSPORT_EXPIRY_DATE,
                        type == Type.PASSPORT
                ))
                .toList();

        JourneyResponse response = journeyService.checkJourney(new JourneyRequest(
                "NO",
                List.of("OSL", "LHR"),
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
                        documents
                )
        ));

        assertEquals(Type.values().length, response.documentCheck().requirements().stream()
                .filter(requirement -> requirement.code().startsWith("DOCUMENT_ACCEPTANCE_"))
                .count());
        assertTrue(response.documentCheck().requirements().stream().anyMatch(requirement ->
                requirement.title().equals("Verify Border crossing card acceptance")
                        && requirement.status() == DocumentRequirement.Status.VERIFY
        ));
    }
}
