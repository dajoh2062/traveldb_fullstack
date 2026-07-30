package projects.traveldbbackend.documents;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ByteArrayResource;
import projects.traveldbbackend.Airport;
import tools.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LocalDocumentRulesProviderTests {

    @Test
    void resolvesDecisionPriorityAndDowngradesRulesPastTheirReviewDate() {
        LocalDocumentRulesProvider provider = provider("""
                {
                  "schemaVersion": 1,
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
        assertTrue(result.warnings().stream().anyMatch(warning -> warning.contains("high-priority")));
    }

    @Test
    void rejectsSnapshotsWithoutHttpsGovernmentSources() {
        assertThrows(IllegalStateException.class, () -> provider("""
                {
                  "schemaVersion": 1,
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

    private LocalDocumentRulesProvider provider(String json) {
        return new LocalDocumentRulesProvider(
                new ConservativeDocumentProvider(),
                new ObjectMapper(),
                new ByteArrayResource(json.getBytes(StandardCharsets.UTF_8))
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
