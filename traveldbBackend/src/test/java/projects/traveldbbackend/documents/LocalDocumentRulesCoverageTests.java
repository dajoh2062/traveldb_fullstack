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

import static org.junit.jupiter.api.Assertions.assertTrue;

class LocalDocumentRulesCoverageTests {

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
                LocalDate.of(2026, 8, 1),
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
