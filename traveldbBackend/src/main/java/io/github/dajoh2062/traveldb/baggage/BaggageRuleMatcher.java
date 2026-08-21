package io.github.dajoh2062.traveldb.baggage;

import io.github.dajoh2062.traveldb.model.Airport;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.Set;

@Component
public class BaggageRuleMatcher {

    private static final Comparator<BaggageRule> RULE_ORDER = Comparator
            .comparingInt(BaggageRule::priority)
            .reversed()
            .thenComparingInt(BaggageRule::position)
            .thenComparing(BaggageRule::id);

    public BaggageAdvice match(
            BaggageRuleSnapshot snapshot,
            BaggageCheckRequest request,
            Airport previous,
            Airport current,
            Airport next
    ) {
        ConnectionFacts facts = new ConnectionFacts(
                !sameCountry(previous, current),
                sameCountry(current, next),
                previous,
                current
        );
        BaggageRule rule = snapshot.rules().stream()
                .filter(candidate -> matches(candidate, snapshot, request, facts))
                .sorted(RULE_ORDER)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "Active baggage-rule dataset has no fallback rule: " + snapshot.datasetVersion()
                ));
        return new BaggageAdvice(
                current.iataCode(),
                rule.status(),
                rule.title(),
                rule.explanation(),
                rule.exceptions(),
                rule.sources(),
                rule.adviceCode()
        );
    }

    private static boolean matches(
            BaggageRule rule,
            BaggageRuleSnapshot snapshot,
            BaggageCheckRequest request,
            ConnectionFacts facts
    ) {
        return matches(rule.enteringCountry(), facts.enteringCountry())
                && matches(rule.onwardDomestic(), facts.onwardDomestic())
                && matches(rule.currentCountryCode(), facts.current().countryCode())
                && matches(rule.currentAirportCode(), facts.current().iataCode())
                && matches(rule.previousAirportCode(), facts.previous().iataCode())
                && matchesAirportGroup(rule.previousAirportGroup(), snapshot, facts.previous().iataCode())
                && matches(rule.ticketArrangement(), request.ticketArrangement())
                && matches(rule.throughCheckStatus(), request.throughCheckStatus());
    }

    private static boolean matches(Boolean expected, boolean actual) {
        return expected == null || expected == actual;
    }

    private static boolean matches(Object expected, Object actual) {
        return expected == null || expected.equals(actual);
    }

    private static boolean matchesAirportGroup(
            String groupCode,
            BaggageRuleSnapshot snapshot,
            String airportCode
    ) {
        if (groupCode == null) {
            return true;
        }
        return snapshot.airportGroups().getOrDefault(groupCode, Set.of()).contains(airportCode);
    }

    private static boolean sameCountry(Airport first, Airport second) {
        return first.countryCode().equalsIgnoreCase(second.countryCode());
    }

    private record ConnectionFacts(
            boolean enteringCountry,
            boolean onwardDomestic,
            Airport previous,
            Airport current
    ) {}
}
