package io.github.dajoh2062.traveldb.baggage;

import io.github.dajoh2062.traveldb.baggage.BaggageAdvice.AdviceCode;
import io.github.dajoh2062.traveldb.baggage.BaggageAdvice.Source;
import io.github.dajoh2062.traveldb.baggage.BaggageAdvice.Status;
import io.github.dajoh2062.traveldb.baggage.BaggageCheckRequest.ThroughCheckStatus;
import io.github.dajoh2062.traveldb.baggage.BaggageCheckRequest.TicketArrangement;
import io.github.dajoh2062.traveldb.model.Airport;
import io.github.dajoh2062.traveldb.support.TestAirports;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BaggageRuleMatcherTests {

    private final BaggageRuleMatcher matcher = new BaggageRuleMatcher();

    @Test
    void givesAHighPriorityBorderRulePrecedenceOverTheFallback() {
        Airport previous = TestAirports.airport("DUB", "IE");
        Airport current = TestAirports.airport("JFK", "US");
        Airport next = TestAirports.airport("BOS", "US");
        BaggageCheckRequest request = new BaggageCheckRequest(
                List.of(previous, current, next),
                true,
                TicketArrangement.SINGLE_BOOKING,
                ThroughCheckStatus.YES
        );
        BaggageRuleSnapshot snapshot = new BaggageRuleSnapshot(
                "test",
                LocalDate.of(2026, 7, 31),
                Map.of("US_PRECLEARANCE", Set.of("DUB")),
                List.of(
                        rule(
                                "preclearance",
                                100,
                                true,
                                "US",
                                "US_PRECLEARANCE",
                                ThroughCheckStatus.YES,
                                AdviceCode.US_PRECLEARANCE_CHECKED_THROUGH,
                                Status.NOT_REQUIRED
                        ),
                        rule(
                                "fallback",
                                0,
                                null,
                                null,
                                null,
                                null,
                                AdviceCode.CHECK_BAGGAGE_TAG,
                                Status.CONFIRM
                        )
                )
        );

        BaggageAdvice advice = matcher.match(snapshot, request, previous, current, next);

        assertEquals(AdviceCode.US_PRECLEARANCE_CHECKED_THROUGH, advice.adviceCode());
        assertEquals(Status.NOT_REQUIRED, advice.status());
    }

    private static BaggageRule rule(
            String id,
            int priority,
            Boolean enteringCountry,
            String currentCountryCode,
            String previousAirportGroup,
            ThroughCheckStatus throughCheckStatus,
            AdviceCode adviceCode,
            Status status
    ) {
        return new BaggageRule(
                id,
                priority,
                priority,
                enteringCountry,
                null,
                currentCountryCode,
                null,
                null,
                previousAirportGroup,
                null,
                throughCheckStatus,
                adviceCode,
                status,
                id,
                id,
                List.of(),
                List.of(new Source("Authority", "https://authority.example/baggage"))
        );
    }
}
