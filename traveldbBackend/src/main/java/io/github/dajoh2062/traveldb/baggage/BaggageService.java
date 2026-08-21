package io.github.dajoh2062.traveldb.baggage;

import io.github.dajoh2062.traveldb.model.Airport;
import org.springframework.stereotype.Service;

@Service
public class BaggageService {

    private final BaggageRuleRepository repository;
    private final BaggageRuleMatcher matcher;
    private volatile BaggageRuleSnapshot snapshot;

    public BaggageService(BaggageRuleRepository repository, BaggageRuleMatcher matcher) {
        this.repository = repository;
        this.matcher = matcher;
    }

    public BaggageCheckResult check(BaggageCheckRequest request) {
        BaggageRuleSnapshot activeRules = activeRules();
        BaggageCheckResult result = new BaggageCheckResult(activeRules.reviewedDate().toString());
        if (!request.hasCheckedBaggage()) {
            result.addNote("No checked baggage was selected, so no baggage reclaim steps apply.");
            return result;
        }

        result.addAssumption("Advice applies to checked baggage and the airports entered in this itinerary.");
        result.addNote("Always read the destination printed on the baggage tag issued at check-in.");
        result.addNote("Airline interline agreements, terminal changes, overnight connections and operational instructions can override the general result.");

        for (int index = 1; index < request.route().size() - 1; index++) {
            Airport previous = request.route().get(index - 1);
            Airport current = request.route().get(index);
            Airport next = request.route().get(index + 1);
            result.addBaggageAdvice(matcher.match(activeRules, request, previous, current, next));
        }
        return result;
    }

    private BaggageRuleSnapshot activeRules() {
        BaggageRuleSnapshot current = snapshot;
        if (current != null) {
            return current;
        }
        synchronized (this) {
            if (snapshot == null) {
                snapshot = repository.findActive().orElseThrow(() ->
                        new IllegalStateException("No active baggage-rule dataset is available."));
            }
            return snapshot;
        }
    }
}
