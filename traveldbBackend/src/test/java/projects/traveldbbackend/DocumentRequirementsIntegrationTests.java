package projects.traveldbbackend;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import projects.traveldbbackend.api.dto.BaggageOptions;
import projects.traveldbbackend.api.dto.DocumentOptions;
import projects.traveldbbackend.api.dto.JourneyRequest;
import projects.traveldbbackend.api.dto.JourneyResponse;
import projects.traveldbbackend.api.dto.TravelDocument;
import projects.traveldbbackend.api.dto.TravelDocument.Type;
import projects.traveldbbackend.documents.DocumentRequirement;
import projects.traveldbbackend.service.TravelService;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
class DocumentRequirementsIntegrationTests {

    private static final LocalDate DEPARTURE_DATE = LocalDate.now().plusMonths(2);
    private static final LocalDate PASSPORT_EXPIRY_DATE = DEPARTURE_DATE.plusYears(3);

    @Autowired
    private TravelService travelService;

    @Test
    void evaluatesDocumentsLocallyWithoutGuessingMissingCountryRules() {
        JourneyResponse response = travelService.checkJourney(
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
                        && requirement.status() == DocumentRequirement.Status.VERIFY));
        assertTrue(response.documentCheck().requirements().stream()
                .anyMatch(requirement -> requirement.scope() == DocumentRequirement.Scope.ENTRY
                        && requirement.countryCode().equals("TH")));
        assertTrue(response.documentCheck().verificationSources().stream()
                .anyMatch(source -> source.url().contains("iatatravelcentre.com")));
        assertFalse(response.documentCheck().verificationSources().stream()
                .anyMatch(source -> source.url().contains("gov.uk")));
        assertTrue(response.documentCheck().warnings().stream()
                .anyMatch(warning -> warning.contains("no external requirements service was contacted")));
    }

    @Test
    void placesUsAndAustralianAuthorizationsAtTheBorderAirports() {
        JourneyResponse response = travelService.checkJourney(
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
    void basicUsEntryStillReturnsTheEstaOrVisaRequirement() {
        JourneyResponse response = travelService.checkJourney(
                request(List.of("OSL", "JFK"), "SINGLE_BOOKING", "YES")
        );

        DocumentRequirement requirement = requirement(response, "ESTA_OR_US_VISA");

        assertEquals(DocumentRequirement.Status.CONDITIONAL, requirement.status());
        assertEquals(DocumentRequirement.Scope.ENTRY, requirement.scope());
        assertEquals("JFK", requirement.airportCode());
        assertEquals("Up to 90 days per admission", factValue(requirement, "Maximum stay"));
        assertEquals("Usually 2 years or until the passport expires",
                factValue(requirement, "ESTA validity"));
        assertEquals("2026-08-09.1", response.documentCheck().datasetVersion());
    }

    @Test
    void appliesUsEstaRulesToPuertoRicoAndTheUsVirginIslands() {
        JourneyResponse puertoRico = travelService.checkJourney(
                request(List.of("OSL", "SJU"), "SINGLE_BOOKING", "YES")
        );
        assertTrue(puertoRico.documentCheck().requirements().stream().anyMatch(requirement ->
                requirement.code().equals("ESTA_OR_US_VISA")
                        && "PR".equals(requirement.countryCode())
                        && "SJU".equals(requirement.airportCode())
        ));

        JourneyResponse virginIslands = travelService.checkJourney(
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
        JourneyResponse response = travelService.checkJourney(
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
        JourneyResponse response = travelService.checkJourney(
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

    @Test
    void returnsTheAustralianEtaPathForAnEligibleUsPassport() {
        JourneyResponse response = travelService.checkJourney(
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
        JourneyResponse response = travelService.checkJourney(
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
        JourneyResponse response = travelService.checkJourney(
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
    void unknownFinalCountryCoverageUsesItsFirstArrivalAirport() {
        JourneyResponse response = travelService.checkJourney(
                request(List.of("OSL", "GRU", "GIG"), "SINGLE_BOOKING", "YES")
        );

        assertTrue(response.documentCheck().requirements().stream().anyMatch(requirement ->
                requirement.code().equals("ENTRY_PERMISSION")
                        && requirement.status() == DocumentRequirement.Status.VERIFY
                        && requirement.scope() == DocumentRequirement.Scope.ENTRY
                        && "BR".equals(requirement.countryCode())
                        && "GRU".equals(requirement.airportCode())
        ));
        assertFalse(response.documentCheck().requirements().stream().anyMatch(requirement ->
                requirement.scope() == DocumentRequirement.Scope.ENTRY
                        && "GIG".equals(requirement.airportCode())
        ));
    }

    @Test
    void keepsReviewedTravelDocumentsForIntraSchengenJourneys() {
        JourneyResponse response = travelService.checkJourney(
                request(List.of("OSL", "CDG"), "SINGLE_BOOKING", "YES")
        );

        assertTrue(response.documentCheck().requirements().stream().anyMatch(requirement ->
                requirement.code().equals("EU_EEA_CH_TRAVEL_DOCUMENT")
                        && requirement.status() == DocumentRequirement.Status.REQUIRED
                        && "CDG".equals(requirement.airportCode())
        ));
        DocumentRequirement permission = requirement(response, "EU_EEA_CH_FREE_MOVEMENT");
        assertEquals(DocumentRequirement.Status.NOT_REQUIRED, permission.status());
        assertEquals("Not required", factValue(permission, "Visa or ETA"));
        assertEquals("Up to 3 months", factValue(permission, "Without residence registration"));
        assertFalse(response.documentActions().contains("EU_EEA_CH_FREE_MOVEMENT"));
    }

    @Test
    void appliesOfficialLocalEtaRulesAndHeldVisaOverrides() {
        JourneyResponse ukEta = travelService.checkJourney(
                documentRequest(List.of("OSL", "LHR"), "TOURISM", List.of())
        );
        DocumentRequirement ukRequirement = requirement(ukEta, "UK_ETA_OR_VISA");
        assertEquals(DocumentRequirement.Status.CONDITIONAL, ukRequirement.status());
        assertTrue(ukRequirement.sources().stream().anyMatch(source -> source.url().contains("gov.uk")));
        assertEquals("Up to 6 months per visit", factValue(ukRequirement, "Maximum stay"));
        assertEquals("2 years or until the passport expires", factValue(ukRequirement, "ETA validity"));
        assertEquals("Valid for the whole stay", factValue(ukRequirement, "Passport validity"));

        JourneyResponse usVisaHolder = travelService.checkJourney(
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
    void distinguishesAustralianElectronicVisaEligibilityBoundaries() {
        JourneyResponse bulgarian = travelService.checkJourney(
                new JourneyRequest("BG", List.of("OSL", "SYD"))
        );
        assertTrue(bulgarian.documentCheck().requirements().stream().anyMatch(requirement ->
                requirement.title().equals("Australian eVisitor or another suitable visa")
                        && "SYD".equals(requirement.airportCode())
        ));

        JourneyResponse vatican = travelService.checkJourney(
                new JourneyRequest("VA", List.of("OSL", "SYD"))
        );
        assertTrue(vatican.documentCheck().requirements().stream().anyMatch(requirement ->
                requirement.title().equals("Australian visa — eVisitor or ETA may be available")
                        && "SYD".equals(requirement.airportCode())
        ));

        JourneyResponse indian = travelService.checkJourney(
                new JourneyRequest("IN", List.of("OSL", "SYD"))
        );
        assertTrue(indian.documentCheck().requirements().stream().anyMatch(requirement ->
                requirement.code().equals("ENTRY_PERMISSION")
                        && requirement.status() == DocumentRequirement.Status.VERIFY
                        && "SYD".equals(requirement.airportCode())
        ));
    }

    @Test
    void evaluatesRepeatedTransitCountriesAsSeparateStops() {
        JourneyResponse response = travelService.checkJourney(
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
    void returnsSpecificCanadianAndNewZealandStayLimits() {
        DocumentRequirement canada = requirement(
                travelService.checkJourney(new JourneyRequest("NO", List.of("OSL", "YYZ"))),
                "CANADA_ETA_OR_VISA"
        );
        assertEquals("Normally up to 6 months per visit", factValue(canada, "Maximum stay"));
        assertEquals("Up to 5 years or until the passport expires", factValue(canada, "eTA validity"));

        DocumentRequirement newZealand = requirement(
                travelService.checkJourney(new JourneyRequest("NO", List.of("OSL", "AKL"))),
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
    void reportsMissingDocumentInputsAndAcceptsACompleteTravelerProfile() {
        JourneyResponse incomplete = travelService.checkJourney(
                request(List.of("OSL", "LHR"), "SINGLE_BOOKING", "YES")
        );
        assertTrue(incomplete.documentCheck().missingInputs().contains("Passport expiry date"));
        assertTrue(incomplete.documentCheck().missingInputs().contains("Departure date"));

        JourneyResponse complete = travelService.checkJourney(
                new JourneyRequest(
                        "NO",
                        List.of("OSL", "LHR"),
                        new BaggageOptions(true, "SINGLE_BOOKING", "YES"),
                        new DocumentOptions(
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

    @Test
    void registersMultipleDocumentsAndUsesARegisteredVisaInExistingRules() {
        JourneyResponse response = travelService.checkJourney(new JourneyRequest(
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
        JourneyResponse response = travelService.checkJourney(new JourneyRequest(
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
        JourneyResponse response = travelService.checkJourney(new JourneyRequest(
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
        JourneyResponse response = travelService.checkJourney(new JourneyRequest(
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
        JourneyResponse response = travelService.checkJourney(new JourneyRequest(
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
    void coversAuditedTaiwanEntryAndBoliviaTransitRules() {
        JourneyResponse taiwanToUk = travelService.checkJourney(
                new JourneyRequest("TW", List.of("TPE", "LHR"))
        );
        assertTrue(taiwanToUk.documentCheck().requirements().stream().anyMatch(requirement ->
                requirement.code().equals("UK_ETA_OR_VISA")
                        && "LHR".equals(requirement.airportCode())
        ));

        JourneyResponse boliviaViaAuckland = travelService.checkJourney(
                new JourneyRequest("BO", List.of("OSL", "AKL", "SYD"))
        );
        assertTrue(boliviaViaAuckland.documentCheck().requirements().stream().anyMatch(requirement ->
                requirement.code().equals("NZETA_OR_TRANSIT_VISA")
                        && "AKL".equals(requirement.airportCode())
        ));
    }

    @Test
    void keepsCanadianEtaPassportQualifiersVisible() {
        JourneyResponse romanianEntry = travelService.checkJourney(
                new JourneyRequest("RO", List.of("OTP", "YYZ"))
        );
        assertTrue(romanianEntry.documentCheck().requirements().stream().anyMatch(requirement ->
                requirement.code().equals("CANADA_ETA_OR_VISA")
                        && requirement.scope() == DocumentRequirement.Scope.ENTRY
                        && requirement.summary().contains("electronic passport")
        ));

        JourneyResponse romanianTransit = travelService.checkJourney(
                new JourneyRequest("RO", List.of("OTP", "YYZ", "JFK"))
        );
        assertTrue(romanianTransit.documentCheck().requirements().stream().anyMatch(requirement ->
                requirement.code().equals("CANADA_ETA_OR_VISA")
                        && requirement.scope() == DocumentRequirement.Scope.TRANSIT
                        && requirement.summary().contains("non-electronic passports")
        ));

        JourneyResponse taiwanEntry = travelService.checkJourney(
                new JourneyRequest("TW", List.of("TPE", "YYZ"))
        );
        assertTrue(taiwanEntry.documentCheck().requirements().stream().anyMatch(requirement ->
                requirement.code().equals("CANADA_ETA_OR_VISA")
                        && requirement.scope() == DocumentRequirement.Scope.ENTRY
                        && requirement.summary().contains("personal identification number")
        ));

        JourneyResponse taiwanTransit = travelService.checkJourney(
                new JourneyRequest("TW", List.of("TPE", "YVR", "LAX"))
        );
        assertTrue(taiwanTransit.documentCheck().requirements().stream().anyMatch(requirement ->
                requirement.code().equals("CANADA_ETA_OR_VISA")
                        && requirement.scope() == DocumentRequirement.Scope.TRANSIT
                        && requirement.summary().contains("personal identification number")
        ));
    }

    @Test
    void aNationalIdentityCardCanUseReviewedRegionalTravelDocumentRules() {
        JourneyResponse response = travelService.checkJourney(new JourneyRequest(
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
        JourneyResponse response = travelService.checkJourney(new JourneyRequest(
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

        JourneyResponse response = travelService.checkJourney(new JourneyRequest(
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

    private JourneyRequest request(
            List<String> route,
            String ticketArrangement,
            String checkedThrough
    ) {
        return new JourneyRequest(
                "NO",
                route,
                new BaggageOptions(true, ticketArrangement, checkedThrough)
        );
    }

    private DocumentRequirement requirement(JourneyResponse response, String code) {
        return response.documentCheck().requirements().stream()
                .filter(requirement -> requirement.code().equals(code))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Missing document requirement " + code));
    }

    private String factValue(DocumentRequirement requirement, String label) {
        return requirement.keyFacts().stream()
                .filter(fact -> fact.label().equals(label))
                .map(DocumentRequirement.KeyFact::value)
                .findFirst()
                .orElseThrow(() -> new AssertionError("Missing key fact " + label));
    }

    private JourneyRequest documentRequest(
            List<String> route,
            String purpose,
            List<String> heldVisas
    ) {
        return new JourneyRequest(
                "NO",
                route,
                new BaggageOptions(true, "SINGLE_BOOKING", "YES"),
                new DocumentOptions(
                        "NO",
                        "NO",
                        PASSPORT_EXPIRY_DATE,
                        DEPARTURE_DATE,
                        purpose,
                        30,
                        List.of(),
                        heldVisas
                )
        );
    }
}
