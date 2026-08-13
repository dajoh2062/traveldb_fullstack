package io.github.dajoh2062.traveldb;

import io.github.dajoh2062.traveldb.api.dto.BaggageOptions;
import io.github.dajoh2062.traveldb.api.dto.DocumentOptions;
import io.github.dajoh2062.traveldb.api.dto.JourneyRequest;
import io.github.dajoh2062.traveldb.api.dto.JourneyResponse;
import io.github.dajoh2062.traveldb.documents.DocumentRequirement;
import io.github.dajoh2062.traveldb.service.JourneyService;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:document-requirements-tests;DB_CLOSE_ON_EXIT=FALSE")
@Import(DocumentRequirementsTestConfiguration.class)
abstract class DocumentRequirementsIntegrationTestSupport {

    protected static final Clock TEST_CLOCK = Clock.fixed(
            Instant.parse("2026-08-13T12:00:00Z"),
            ZoneOffset.UTC
    );
    protected static final LocalDate DEPARTURE_DATE = LocalDate.now(TEST_CLOCK).plusMonths(2);
    protected static final LocalDate PASSPORT_EXPIRY_DATE = DEPARTURE_DATE.plusYears(3);

    @Autowired
    protected JourneyService journeyService;

    protected JourneyRequest request(
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

    protected DocumentRequirement requirement(JourneyResponse response, String code) {
        return response.documentCheck().requirements().stream()
                .filter(requirement -> requirement.code().equals(code))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Missing document requirement " + code));
    }

    protected boolean hasEntryFallback(JourneyResponse response, String airportCode) {
        return response.documentCheck().requirements().stream().anyMatch(requirement ->
                requirement.code().equals("ENTRY_PERMISSION")
                        && requirement.scope() == DocumentRequirement.Scope.ENTRY
                        && airportCode.equals(requirement.airportCode())
        );
    }

    protected String factValue(DocumentRequirement requirement, String label) {
        return requirement.keyFacts().stream()
                .filter(fact -> fact.label().equals(label))
                .map(DocumentRequirement.KeyFact::value)
                .findFirst()
                .orElseThrow(() -> new AssertionError("Missing key fact " + label));
    }

    protected JourneyRequest documentRequest(
            List<String> route,
            String purpose,
            List<String> heldVisas
    ) {
        return documentRequest("NO", route, purpose, heldVisas);
    }

    protected JourneyRequest documentRequest(
            String nationality,
            List<String> route,
            String purpose,
            List<String> heldVisas
    ) {
        return new JourneyRequest(
                nationality,
                route,
                new BaggageOptions(true, "SINGLE_BOOKING", "YES"),
                new DocumentOptions(
                        nationality,
                        nationality,
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
