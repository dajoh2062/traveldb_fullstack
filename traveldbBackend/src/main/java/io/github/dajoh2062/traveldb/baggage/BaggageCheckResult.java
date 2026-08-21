package io.github.dajoh2062.traveldb.baggage;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class BaggageCheckResult {

    private final String guidanceReviewedDate;
    private final Set<String> baggagePickupAt = new LinkedHashSet<>();
    private final List<BaggageAdvice> baggageAdvice = new ArrayList<>();
    private final Set<String> assumptions = new LinkedHashSet<>();
    private final Set<String> notes = new LinkedHashSet<>();

    BaggageCheckResult(String guidanceReviewedDate) {
        this.guidanceReviewedDate = guidanceReviewedDate;
    }

    public String guidanceReviewedDate() {
        return guidanceReviewedDate;
    }

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

    void addBaggagePickupAt(String airportCode) {
        if (airportCode != null && !airportCode.isBlank()) {
            baggagePickupAt.add(airportCode.trim().toUpperCase(Locale.ROOT));
        }
    }

    void addBaggageAdvice(BaggageAdvice advice) {
        if (advice == null || advice.airportCode() == null || advice.airportCode().isBlank()) {
            return;
        }

        baggageAdvice.add(advice);
        if (advice.status() == BaggageAdvice.Status.REQUIRED) {
            addBaggagePickupAt(advice.airportCode());
        }
    }

    void addAssumption(String assumption) {
        if (assumption != null && !assumption.isBlank()) {
            assumptions.add(assumption.trim());
        }
    }

    void addNote(String note) {
        if (note != null && !note.isBlank()) {
            notes.add(note.trim());
        }
    }
}
