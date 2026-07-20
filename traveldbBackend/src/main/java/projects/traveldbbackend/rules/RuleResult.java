package projects.traveldbbackend.rules;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class RuleResult {

    private final Set<String> requiredDocuments = new LinkedHashSet<>();
    private final Set<String> baggagePickupAt = new LinkedHashSet<>();
    private final List<BaggageAdvice> baggageAdvice = new ArrayList<>();
    private final Set<String> assumptions = new LinkedHashSet<>();
    private final Set<String> notes = new LinkedHashSet<>();

    public Set<String> requiredDocuments() {
        return requiredDocuments;
    }

    public Set<String> baggagePickupAt() {
        return baggagePickupAt;
    }

    public List<BaggageAdvice> baggageAdvice() {
        return List.copyOf(baggageAdvice);
    }

    public Set<String> assumptions() {
        return assumptions;
    }

    public Set<String> notes() {
        return notes;
    }

    public void addDocument(String doc) {
        if (doc != null && !doc.isBlank()) {
            requiredDocuments.add(doc.trim());
        }
    }

    public void addBaggagePickupAt(String iata) {
        if (iata != null && !iata.isBlank()) {
            baggagePickupAt.add(iata.trim().toUpperCase());
        }
    }

    public void addBaggageAdvice(BaggageAdvice advice) {
        if (advice == null || advice.airportCode() == null || advice.airportCode().isBlank()) {
            return;
        }

        baggageAdvice.add(advice);
        if (advice.status() == BaggageAdvice.Status.REQUIRED) {
            addBaggagePickupAt(advice.airportCode());
        }
    }

    public void addAssumption(String a) {
        if (a != null && !a.isBlank()) {
            assumptions.add(a.trim());
        }
    }

    public void addNote(String n) {
        if (n != null && !n.isBlank()) {
            notes.add(n.trim());
        }
    }
}
