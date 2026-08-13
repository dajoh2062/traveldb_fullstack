package io.github.dajoh2062.traveldb.documents;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.UrlResource;
import io.github.dajoh2062.traveldb.api.dto.TravelDocument;
import io.github.dajoh2062.traveldb.model.Airport;
import tools.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LocalDocumentRulesProviderTests {

    @Test
    void resolvesDecisionPriorityAndDowngradesRulesPastTheirReviewDate() {
        LocalDocumentRulesProvider provider = provider("""
                {
                  "schemaVersion": 2,
                  "datasetVersion": "test-1",
                  "generatedAt": "2026-01-01T00:00:00Z",
                  "sources": [{"label":"Authority","url":"https://authority.example/rules","sourceType":"GOVERNMENT"}],
                  "rules": [
                    {
                      "id":"low-priority",
                      "decisionKey":"XY_ENTRY",
                      "scope":"ENTRY",
                      "destinationCountries":["XY"],
                      "nationalities":["NO"],
                      "priority":10,
                      "effectiveFrom":"2020-01-01",
                      "lastVerified":"2026-01-01",
                      "reviewAfter":"2030-01-01",
                      "output":{
                        "code":"LOW",
                        "category":"VISA",
                        "status":"CONDITIONAL",
                        "title":"Low priority",
                        "summary":"Should be overridden.",
                        "conditions":[],
                        "sources":[{"label":"Authority","url":"https://authority.example/low","sourceType":"GOVERNMENT"}]
                      }
                    },
                    {
                      "id":"high-priority",
                      "decisionKey":"XY_ENTRY",
                      "scope":"ENTRY",
                      "destinationCountries":["XY"],
                      "nationalities":["NO"],
                      "priority":20,
                      "effectiveFrom":"2020-01-01",
                      "lastVerified":"2020-01-01",
                      "reviewAfter":"2020-06-01",
                      "output":{
                        "code":"HIGH",
                        "category":"VISA",
                        "status":"CONDITIONAL",
                        "title":"High priority",
                        "summary":"Stored decision.",
                        "conditions":["Preserve sentence casing."],
                        "keyFacts":[{"label":"Maximum stay","value":"Up to 30 days"}],
                        "sources":[{"label":"Authority","url":"https://authority.example/high","sourceType":"GOVERNMENT"}]
                      }
                    }
                  ]
                }
                """);

        DocumentCheckResult result = provider.check(input());
        List<DocumentRequirement> matched = result.requirements().stream()
                .filter(requirement -> requirement.code().equals("HIGH") || requirement.code().equals("LOW"))
                .toList();

        assertEquals(1, matched.size());
        assertEquals("HIGH", matched.getFirst().code());
        assertEquals(DocumentRequirement.Status.VERIFY, matched.getFirst().status());
        assertTrue(matched.getFirst().conditions().contains("Preserve sentence casing."));
        assertEquals(List.of(new DocumentRequirement.KeyFact("Maximum stay", "Up to 30 days")),
                matched.getFirst().keyFacts());
        assertEquals(LocalDate.of(2020, 1, 1), matched.getFirst().lastVerified());
        assertEquals(LocalDate.of(2020, 6, 1), matched.getFirst().reviewAfter());
        assertEquals("test-1", result.datasetVersion());
        assertTrue(result.warnings().stream().anyMatch(warning -> warning.contains("high-priority")));
    }

    @Test
    void rejectsSnapshotsWithoutHttpsGovernmentSources() {
        assertThrows(IllegalStateException.class, () -> provider("""
                {
                  "schemaVersion": 2,
                  "datasetVersion": "invalid",
                  "generatedAt": "2026-01-01T00:00:00Z",
                  "sources": [],
                  "rules": [{
                    "id":"invalid-source",
                    "decisionKey":"XY_ENTRY",
                    "scope":"ENTRY",
                    "destinationCountries":["XY"],
                    "nationalities":["*"],
                    "priority":1,
                    "lastVerified":"2026-01-01",
                    "reviewAfter":"2030-01-01",
                    "output":{
                      "code":"INVALID",
                      "category":"VISA",
                      "status":"VERIFY",
                      "title":"Invalid",
                      "summary":"Invalid source.",
                      "conditions":[],
                      "sources":[{"label":"Bad source","url":"http://example.com","sourceType":"GOVERNMENT"}]
                    }
                  }]
                }
                """));
    }

    @Test
    void rejectsMalformedRequiredDestinationAndNationalitySelectors() {
        for (String selectors : List.of(
                "\"destinationCountries\":\"XY\",\"nationalities\":[\"NO\"]",
                "\"destinationCountries\":[\"XY\"],\"nationalities\":\"NO\""
        )) {
            IllegalStateException error = assertThrows(
                    IllegalStateException.class,
                    () -> provider(snapshotWithSelectors(selectors))
            );

            assertTrue(error.getCause().getMessage().contains("selector must be an array"));
        }
    }

    @Test
    void rejectsMalformedOptionalSelectorsInsteadOfTreatingThemAsWildcards() {
        IllegalStateException error = assertThrows(
                IllegalStateException.class,
                () -> provider(snapshotWithSelectors(
                        "\"destinationCountries\":[\"XY\"],"
                                + "\"nationalities\":[\"NO\"],"
                                + "\"travelPurposes\":\"TOURISM\""
                ))
        );

        assertTrue(error.getCause().getMessage().contains("selector must be an array: travelPurposes"));
    }

    @Test
    void rejectsRequiredSelectorValuesThatCouldBroadenAResult() {
        for (String selectors : List.of(
                "\"nationalities\":[\"NO\"]",
                "\"destinationCountries\":[],\"nationalities\":[\"NO\"]",
                "\"destinationCountries\":[\"xy\"],\"nationalities\":[\"NO\"]",
                "\"destinationCountries\":[\"XY\",\"XY\"],\"nationalities\":[\"NO\"]",
                "\"destinationCountries\":[\"*\",\"XY\"],\"nationalities\":[\"NO\"]"
        )) {
            assertThrows(
                    IllegalStateException.class,
                    () -> provider(snapshotWithSelectors(selectors))
            );
        }
    }

    @Test
    void rejectsMalformedApplicabilityAndOutputFields() {
        String valid = snapshotWithSelectors(
                "\"destinationCountries\":[\"XY\"],\"nationalities\":[\"NO\"]"
        );

        for (String snapshot : List.of(
                valid.replace("\"schemaVersion\": 2", "\"schemaVersion\": \"2\""),
                valid.replace("\"priority\":1", "\"priority\":\"1\""),
                valid.replace("\"priority\":1", "\"minimumAge\":\"18\",\"priority\":1"),
                valid.replace("\"conditions\":[]", "\"conditions\":\"none\""),
                valid.replace("\"lastVerified\":\"2026-08-09\"", "\"lastVerified\":20260809"),
                valid.replace("\"title\":\"Invalid selectors\"", "\"title\":123"),
                valid.replace(
                        "\"conditions\":[]",
                        "\"conditions\":[],\"keyFacts\":[{\"label\":\"Visa\",\"value\":\"Required\"},"
                                + "{\"label\":\"visa\",\"value\":\"Not required\"}]"
                ),
                valid.replace(
                        "\"conditions\":[]",
                        "\"conditions\":[],\"keyFacts\":["
                                + "{\"label\":\"1\",\"value\":\"1\"},"
                                + "{\"label\":\"2\",\"value\":\"2\"},"
                                + "{\"label\":\"3\",\"value\":\"3\"},"
                                + "{\"label\":\"4\",\"value\":\"4\"},"
                                + "{\"label\":\"5\",\"value\":\"5\"},"
                                + "{\"label\":\"6\",\"value\":\"6\"},"
                                + "{\"label\":\"7\",\"value\":\"7\"}]"
                ),
                valid.replace(
                        "\"sources\":[{\"label\":\"Authority\",\"url\":\"https://authority.example/rules\",\"sourceType\":\"GOVERNMENT\"}]",
                        "\"sources\":[]"
                )
        )) {
            assertThrows(IllegalStateException.class, () -> provider(snapshot));
        }
    }

    @Test
    void rejectsNonFileUrlRuleResourcesBeforeAttemptingToDownloadThem() {
        assertThrows(IllegalStateException.class, () -> new LocalDocumentRulesProvider(
                new ConservativeDocumentProvider(Clock.systemUTC()),
                new ObjectMapper(),
                new UrlResource("https://authority.example/document-rules.json"),
                Clock.systemUTC()
        ));
        assertThrows(IllegalStateException.class, () -> new LocalDocumentRulesProvider(
                new ConservativeDocumentProvider(Clock.systemUTC()),
                new ObjectMapper(),
                new UrlResource("ftp://authority.example/document-rules.json"),
                Clock.systemUTC()
        ));
    }

    @Test
    void aDocumentOnlyRuleCannotSuppressAnUnknownEntryPermissionDecision() {
        LocalDocumentRulesProvider provider = provider("""
                {
                  "schemaVersion": 2,
                  "datasetVersion": "test-document-only",
                  "generatedAt": "2026-08-09T00:00:00Z",
                  "sources": [{"label":"Authority","url":"https://authority.example/rules","sourceType":"GOVERNMENT"}],
                  "rules": [{
                    "id":"passport-rule",
                    "decisionKey":"XY_TRAVEL_DOCUMENT",
                    "scope":"ENTRY",
                    "destinationCountries":["XY"],
                    "nationalities":["NO"],
                    "priority":10,
                    "lastVerified":"2026-08-09",
                    "reviewAfter":"2026-12-01",
                    "output":{
                      "code":"PASSPORT_RULE",
                      "category":"TRAVEL_DOCUMENT",
                      "status":"REQUIRED",
                      "title":"Carry a passport",
                      "summary":"A passport is required.",
                      "conditions":[],
                      "sources":[{"label":"Authority","url":"https://authority.example/passports","sourceType":"GOVERNMENT"}]
                    }
                  }]
                }
                """);

        DocumentCheckResult result = provider.check(input());

        assertTrue(result.requirements().stream().anyMatch(requirement ->
                requirement.code().equals("PASSPORT_RULE")
        ));
        assertTrue(result.requirements().stream().anyMatch(requirement ->
                requirement.code().equals("ENTRY_PERMISSION")
                        && requirement.status() == DocumentRequirement.Status.VERIFY
                        && "BBB".equals(requirement.airportCode())
        ));
        assertEquals("LOCAL_RULES_WITH_VERIFY_FALLBACK", result.coverage());
    }

    @Test
    void keepsPerDocumentVerificationGuidanceAlongsideLocalJourneyRules() {
        LocalDocumentRulesProvider provider = provider("""
                {
                  "schemaVersion": 2,
                  "datasetVersion": "test-journey-guidance",
                  "generatedAt": "2026-01-01T00:00:00Z",
                  "sources": [{"label":"Authority","url":"https://authority.example/rules","sourceType":"GOVERNMENT"}],
                  "rules": [{
                    "id":"journey-rule",
                    "decisionKey":"JOURNEY_DOCUMENT",
                    "scope":"JOURNEY",
                    "destinationCountries":["*"],
                    "nationalities":["NO"],
                    "priority":10,
                    "lastVerified":"2026-01-01",
                    "reviewAfter":"2030-01-01",
                    "output":{
                      "code":"LOCAL_JOURNEY_RULE",
                      "category":"PASSPORT_VALIDITY",
                      "status":"VERIFY",
                      "title":"Local journey rule",
                      "summary":"Stored journey guidance.",
                      "conditions":[],
                      "sources":[{"label":"Authority","url":"https://authority.example/journey","sourceType":"GOVERNMENT"}]
                    }
                  }]
                }
                """);
        DocumentCheckInput input = new DocumentCheckInput(
                "NO",
                List.of(airport("AAA", "NO"), airport("BBB", "XY")),
                "NO",
                "NO",
                LocalDate.of(2030, 1, 1),
                LocalDate.of(2026, 8, 1),
                "TOURISM",
                30,
                List.of(),
                List.of(),
                List.of(),
                List.of(new TravelDocument(
                        "PASSPORT",
                        null,
                        "NO",
                        LocalDate.of(2030, 1, 1),
                        true
                ))
        );

        DocumentCheckResult result = provider.check(input);

        assertTrue(result.requirements().stream()
                .anyMatch(requirement -> requirement.code().equals("LOCAL_JOURNEY_RULE")));
        assertTrue(result.requirements().stream().anyMatch(requirement ->
                requirement.code().equals("DOCUMENT_ACCEPTANCE_1")
                        && requirement.status() == DocumentRequirement.Status.VERIFY
        ));
    }

    private LocalDocumentRulesProvider provider(String json) {
        return new LocalDocumentRulesProvider(
                new ConservativeDocumentProvider(Clock.systemUTC()),
                new ObjectMapper(),
                new ByteArrayResource(json.getBytes(StandardCharsets.UTF_8)),
                Clock.fixed(Instant.parse("2026-08-09T12:00:00Z"), ZoneOffset.UTC)
        );
    }

    private String snapshotWithSelectors(String selectors) {
        return """
                {
                  "schemaVersion": 2,
                  "datasetVersion": "invalid-selectors",
                  "generatedAt": "2026-08-09T00:00:00Z",
                  "sources": [{"label":"Authority","url":"https://authority.example/rules","sourceType":"GOVERNMENT"}],
                  "rules": [{
                    "id":"invalid-selectors",
                    "decisionKey":"XY_ENTRY",
                    "scope":"ENTRY",
                    %s,
                    "priority":1,
                    "lastVerified":"2026-08-09",
                    "reviewAfter":"2026-12-01",
                    "output":{
                      "code":"INVALID_SELECTORS",
                      "category":"VISA",
                      "status":"NOT_REQUIRED",
                      "title":"Invalid selectors",
                      "summary":"This rule must never be broadened.",
                      "conditions":[],
                      "sources":[{"label":"Authority","url":"https://authority.example/rules","sourceType":"GOVERNMENT"}]
                    }
                  }]
                }
                """.formatted(selectors);
    }

    private DocumentCheckInput input() {
        Airport origin = airport("AAA", "NO");
        Airport destination = airport("BBB", "XY");
        return new DocumentCheckInput(
                "NO",
                List.of(origin, destination),
                "NO",
                "NO",
                LocalDate.of(2030, 1, 1),
                LocalDate.of(2026, 8, 1),
                "TOURISM",
                30,
                List.of(),
                List.of()
        );
    }

    private Airport airport(String iata, String countryCode) {
        return io.github.dajoh2062.traveldb.support.TestAirports.airport(iata, countryCode);
    }
}
