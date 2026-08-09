package projects.traveldbbackend.documents;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import projects.traveldbbackend.model.Airport;
import projects.traveldbbackend.documents.DocumentRequirement.Category;
import projects.traveldbbackend.documents.DocumentRequirement.Scope;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LocalDocumentRulesCoverageTests {

    private static final List<String> SCHENGEN_COUNTRIES = List.of(
            "AT", "BE", "BG", "CH", "CZ", "DE", "DK", "EE", "ES", "FI",
            "FR", "GR", "HR", "HU", "IS", "IT", "LI", "LT", "LU", "LV",
            "MT", "NL", "NO", "PL", "PT", "RO", "SE", "SI", "SK"
    );

    private static final List<String> JAPAN_UNCONDITIONAL_WAIVER_COUNTRIES = List.of(
            "KR", "SG", "CA", "US", "AR", "BS", "CL", "CR", "DO", "SV", "GT", "HN", "SR",
            "AU", "NZ", "IL", "MU", "TN", "AD", "BE", "BG", "HR", "CY", "CZ", "DK", "EE",
            "FI", "FR", "GR", "HU", "IS", "IT", "LV", "LT", "LU", "MT", "MC", "NL", "MK",
            "NO", "PL", "PT", "RO", "SM", "SK", "SI", "ES", "SE", "AT", "DE", "IE", "LI",
            "MX", "CH", "BN"
    );

    private static final List<String> JAPAN_CONDITIONAL_WAIVER_COUNTRIES = List.of(
            "GB", "ID", "TH", "QA", "ME", "PE", "PY", "PA", "BR", "AE", "RS", "MY", "BB",
            "LS", "TR", "TW", "HK", "MO", "UY"
    );

    private static final List<String> JAPAN_SHORT_STAY_PURPOSES = List.of(
            "TOURISM", "BUSINESS", "VISIT", "TRANSIT"
    );

    @Test
    void everyIsoPassportAndCountryPairReturnsAReviewedRuleOrVerifyFallback() {
        LocalDocumentRulesProvider provider = new LocalDocumentRulesProvider(
                new ConservativeDocumentProvider(),
                new ObjectMapper(),
                new ClassPathResource("data/document-rules.json")
        );
        List<String> missing = new ArrayList<>();

        for (String nationality : Arrays.asList(Locale.getISOCountries())) {
            for (String destination : Arrays.asList(Locale.getISOCountries())) {
                String bridgeCountry = "US".equals(destination) ? "CA" : "US";
                DocumentCheckResult result = provider.check(input(
                        nationality,
                        List.of(
                                airport("ORG", "ZZ"),
                                airport("TRN", destination),
                                airport("BRG", bridgeCountry),
                                airport("ENT", destination)
                        )
                ));

                if (!hasReviewedRuleOrVerifyFallback(result, destination, "TRN", Scope.TRANSIT)) {
                    missing.add(nationality + "->" + destination + ":TRANSIT");
                }
                if (!hasReviewedRuleOrVerifyFallback(result, destination, "ENT", Scope.ENTRY)) {
                    missing.add(nationality + "->" + destination + ":ENTRY");
                }
            }
        }

        assertTrue(
                missing.isEmpty(),
                () -> "Missing fail-closed document decisions: "
                        + String.join(", ", missing.stream().limit(30).toList())
        );
    }

    @Test
    void everySchengenCountryUsesTheGroupedOrdinaryPassportRules() {
        LocalDocumentRulesProvider provider = new LocalDocumentRulesProvider(
                new ConservativeDocumentProvider(),
                new ObjectMapper(),
                new ClassPathResource("data/document-rules.json")
        );
        List<String> missing = new ArrayList<>();

        for (String destination : SCHENGEN_COUNTRIES) {
            assertSchengenRule(
                    provider,
                    destination,
                    "NO",
                    "EU_EEA_CH_FREE_MOVEMENT",
                    DocumentRequirement.Status.NOT_REQUIRED,
                    missing
            );
            assertSchengenRule(
                    provider,
                    destination,
                    "US",
                    "SCHENGEN_SHORT_STAY_VISA_EXEMPT",
                    DocumentRequirement.Status.NOT_REQUIRED,
                    missing
            );
            assertSchengenRule(
                    provider,
                    destination,
                    "IN",
                    "SCHENGEN_SHORT_STAY_VISA_REQUIRED",
                    DocumentRequirement.Status.CONDITIONAL,
                    missing
            );
        }

        assertTrue(missing.isEmpty(), () -> "Missing grouped Schengen rules: " + String.join(", ", missing));
    }

    @Test
    void everyReviewedJapanWaiverNationalityResolvesBeforeTheWildcard() {
        LocalDocumentRulesProvider provider = new LocalDocumentRulesProvider(
                new ConservativeDocumentProvider(),
                new ObjectMapper(),
                new ClassPathResource("data/document-rules.json")
        );
        List<String> missing = new ArrayList<>();
        List<String> allWaivers = new ArrayList<>(JAPAN_UNCONDITIONAL_WAIVER_COUNTRIES);
        allWaivers.addAll(JAPAN_CONDITIONAL_WAIVER_COUNTRIES);

        assertEquals(55, JAPAN_UNCONDITIONAL_WAIVER_COUNTRIES.size());
        assertEquals(19, JAPAN_CONDITIONAL_WAIVER_COUNTRIES.size());
        assertEquals(74, allWaivers.size());
        assertEquals(74, allWaivers.stream().distinct().count());

        for (String nationality : JAPAN_UNCONDITIONAL_WAIVER_COUNTRIES) {
            assertJapanRule(provider, nationality, DocumentRequirement.Status.NOT_REQUIRED, missing);
        }
        for (String nationality : JAPAN_CONDITIONAL_WAIVER_COUNTRIES) {
            assertJapanRule(provider, nationality, DocumentRequirement.Status.CONDITIONAL, missing);
        }

        assertTrue(missing.isEmpty(), () -> "Missing reviewed Japan waiver rules: " + String.join(", ", missing));
    }

    @Test
    void japanShortStaySelectorsRequireAnExplicitSupportedPurpose() {
        DocumentRuleSnapshot snapshot = DocumentRuleSnapshotLoader.load(
                new ObjectMapper(),
                new ClassPathResource("data/document-rules.json")
        );

        List<DocumentRuleSnapshot.Rule> purposeScopedRules = snapshot.rules().stream()
                .filter(rule -> rule.destinationCountries().contains("JP"))
                .filter(rule -> "JAPAN_SHORT_STAY_PERMISSION".equals(rule.code())
                        || "japan-ordinary-passport-non-waiver-check".equals(rule.id()))
                .toList();

        assertEquals(22, purposeScopedRules.size());
        assertTrue(purposeScopedRules.stream().allMatch(rule ->
                JAPAN_SHORT_STAY_PURPOSES.equals(rule.travelPurposes())
        ));

        for (String purposeIndependentRule : List.of(
                "japan-citizen-return",
                "japan-valid-visa-entry",
                "japan-entry-valid-passport"
        )) {
            DocumentRuleSnapshot.Rule rule = snapshot.rules().stream()
                    .filter(candidate -> purposeIndependentRule.equals(candidate.id()))
                    .findFirst()
                    .orElseThrow();
            assertTrue(rule.travelPurposes().isEmpty());
        }

        DocumentRuleSnapshot.Rule heldVisa = snapshot.rules().stream()
                .filter(rule -> "japan-valid-visa-entry".equals(rule.id()))
                .findFirst()
                .orElseThrow();
        assertEquals(List.of("JP"), heldVisa.excludedNationalities());
    }

    @Test
    void everyIsoOrdinaryPassportIssuerGetsAJapanSpecificEntryDecision() {
        LocalDocumentRulesProvider provider = new LocalDocumentRulesProvider(
                new ConservativeDocumentProvider(),
                new ObjectMapper(),
                new ClassPathResource("data/document-rules.json")
        );
        List<String> missing = new ArrayList<>();

        for (String nationality : Arrays.asList(Locale.getISOCountries())) {
            DocumentCheckResult result = provider.check(input(
                    nationality,
                    List.of(airport("ORG", "ZZ"), airport("NRT", "JP"))
            ));
            String expectedCode;
            DocumentRequirement.Status expectedStatus;
            if (JAPAN_UNCONDITIONAL_WAIVER_COUNTRIES.contains(nationality)) {
                expectedCode = "JAPAN_SHORT_STAY_PERMISSION";
                expectedStatus = DocumentRequirement.Status.NOT_REQUIRED;
            } else if (JAPAN_CONDITIONAL_WAIVER_COUNTRIES.contains(nationality)) {
                expectedCode = "JAPAN_SHORT_STAY_PERMISSION";
                expectedStatus = DocumentRequirement.Status.CONDITIONAL;
            } else if ("JP".equals(nationality)) {
                expectedCode = "JAPAN_CITIZEN_RETURN";
                expectedStatus = DocumentRequirement.Status.NOT_REQUIRED;
            } else {
                expectedCode = "JAPAN_VISA_OR_SPECIAL_STATUS_CHECK";
                expectedStatus = DocumentRequirement.Status.CONDITIONAL;
            }

            boolean matched = result.requirements().stream().anyMatch(requirement ->
                    expectedCode.equals(requirement.code())
                            && expectedStatus == requirement.status()
                            && "JP".equals(requirement.countryCode())
                            && "NRT".equals(requirement.airportCode())
                            && requirement.sources().stream().allMatch(source ->
                            "GOVERNMENT".equals(source.sourceType()))
            );
            boolean hasFallback = result.requirements().stream().anyMatch(requirement ->
                    "ENTRY_PERMISSION".equals(requirement.code())
                            && "NRT".equals(requirement.airportCode())
            );
            if (!matched || hasFallback) {
                missing.add(nationality + ":" + expectedCode);
            }
        }

        assertTrue(missing.isEmpty(), () ->
                "Missing Japan issuer decisions: " + String.join(", ", missing.stream().limit(30).toList()));
    }

    private void assertJapanRule(
            LocalDocumentRulesProvider provider,
            String nationality,
            DocumentRequirement.Status expectedStatus,
            List<String> missing
    ) {
        DocumentCheckResult result = provider.check(input(
                nationality,
                List.of(airport("ORG", "ZZ"), airport("NRT", "JP"))
        ));
        boolean matched = result.requirements().stream().anyMatch(requirement ->
                "JAPAN_SHORT_STAY_PERMISSION".equals(requirement.code())
                        && expectedStatus == requirement.status()
                        && "JP".equals(requirement.countryCode())
                        && "NRT".equals(requirement.airportCode())
        );
        boolean wildcardSelected = result.requirements().stream().anyMatch(requirement ->
                "JAPAN_VISA_OR_SPECIAL_STATUS_CHECK".equals(requirement.code())
        );
        if (!matched || wildcardSelected) {
            missing.add(nationality + ":" + expectedStatus);
        }
    }

    private void assertSchengenRule(
            LocalDocumentRulesProvider provider,
            String destination,
            String nationality,
            String code,
            DocumentRequirement.Status status,
            List<String> missing
    ) {
        DocumentCheckResult result = provider.check(input(
                nationality,
                List.of(airport("ORG", "ZZ"), airport("ENT", destination))
        ));
        boolean matched = result.requirements().stream().anyMatch(requirement ->
                code.equals(requirement.code())
                        && status == requirement.status()
                        && destination.equals(requirement.countryCode())
                        && "ENT".equals(requirement.airportCode())
        );
        if (!matched) {
            missing.add(nationality + "->" + destination + ":" + code);
        }
    }

    private boolean hasReviewedRuleOrVerifyFallback(
            DocumentCheckResult result,
            String countryCode,
            String airportCode,
            Scope scope
    ) {
        String fallbackCode = scope == Scope.TRANSIT ? "TRANSIT_PERMISSION" : "ENTRY_PERMISSION";
        return result.requirements().stream().anyMatch(requirement -> {
            if (requirement.scope() != scope
                    || !countryCode.equals(requirement.countryCode())
                    || !airportCode.equals(requirement.airportCode())
                    || "ENTRY_CONDITIONS".equals(requirement.code())) {
                return false;
            }

            if (fallbackCode.equals(requirement.code())) {
                return requirement.status() == DocumentRequirement.Status.VERIFY;
            }
            return isPermissionCategory(requirement.category())
                    && !requirement.sources().isEmpty()
                    && requirement.sources().stream()
                    .allMatch(source -> "GOVERNMENT".equals(source.sourceType()));
        });
    }

    private boolean isPermissionCategory(Category category) {
        return category == Category.VISA
                || category == Category.ELECTRONIC_AUTHORIZATION
                || category == Category.TRANSIT_PERMISSION;
    }

    private DocumentCheckInput input(String nationality, List<Airport> route) {
        return new DocumentCheckInput(
                nationality,
                route,
                nationality,
                nationality,
                LocalDate.of(2030, 1, 1),
                LocalDate.of(2026, 8, 9),
                "TOURISM",
                30,
                List.of(),
                List.of()
        );
    }

    private Airport airport(String iataCode, String countryCode) {
        Airport airport = new Airport();
        airport.setIataCode(iataCode);
        airport.setCountryCode(countryCode);
        airport.setCountry(countryCode);
        return airport;
    }
}
