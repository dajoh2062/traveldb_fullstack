package projects.traveldbbackend.api;

import java.util.List;

public final class InvalidJourneyRequestException extends RuntimeException {

    private final List<FieldViolation> violations;

    public InvalidJourneyRequestException(List<FieldViolation> violations) {
        super("The journey request contains invalid fields.");
        this.violations = List.copyOf(violations);
    }

    public List<FieldViolation> getViolations() {
        return violations;
    }

    public record FieldViolation(String field, String message) {}
}
