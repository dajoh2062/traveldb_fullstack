package projects.traveldbbackend.documents;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.UrlResource;
import projects.traveldbbackend.api.dto.TravelDocument;
import projects.traveldbbackend.model.Airport;
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
    void rejectsNonFileUrlRuleResourcesBeforeAttemptingToDownloadThem() {
        assertThrows(IllegalStateException.class, () -> new LocalDocumentRulesProvider(
                new ConservativeDocumentProvider(),
                new ObjectMapper(),
                new UrlResource("https://authority.example/document-rules.json")
        ));
        assertThrows(IllegalStateException.class, () -> new LocalDocumentRulesProvider(
                new ConservativeDocumentProvider(),
                new ObjectMapper(),
                new UrlResource("ftp://authority.example/document-rules.json")
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
                new ConservativeDocumentProvider(),
                new ObjectMapper(),
                new ByteArrayResource(json.getBytes(StandardCharsets.UTF_8)),
                Clock.fixed(Instant.parse("2026-08-09T12:00:00Z"), ZoneOffset.UTC)
        );
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
        Airport airport = new Airport();
        airport.setIataCode(iata);
        airport.setCountryCode(countryCode);
        airport.setCountry(countryCode);
        return airport;
    }
}
