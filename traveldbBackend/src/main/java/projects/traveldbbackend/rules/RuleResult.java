package projects.traveldbbackend.rules;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class RuleResult {

    private final Set<String> baggagePickupAt = new LinkedHashSet<>();
    private final List<BaggageAdvice> baggageAdvice = new ArrayList<>();
    private final Set<String> assumptions = new LinkedHashSet<>();
    private final Set<String> notes = new LinkedHashSet<>();

    public Set<String> baggagePickupAt() {
        return Collections.unmodifiableSet(baggagePickupAt);
    }

    public List<BaggageAdvice> baggageAdvice() {
        return List.copyOf(baggageAdvice);
    }

    public Set<String> assumptions() {
        return Collections.unmodifiableSet(assumptions);
    }

    public Set<String> notes() {
        return Collections.unmodifiableSet(notes);
    }

    public void addBaggagePickupAt(String airportCode) {
        if (airportCode != null && !airportCode.isBlank()) {
            baggagePickupAt.add(airportCode.trim().toUpperCase(Locale.ROOT));
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

    public void addAssumption(String assumption) {
        if (assumption != null && !assumption.isBlank()) {
            assumptions.add(assumption.trim());
        }
    }

    public void addNote(String note) {
        if (note != null && !note.isBlank()) {
            notes.add(note.trim());
        }
    }
}
