package projects.traveldbbackend.rules;

import java.util.List;

public record BaggageAdvice(
        String airportCode,
        Status status,
        String title,
        String explanation,
        List<String> exceptions,
        List<Source> sources
) {
    public enum Status {
        REQUIRED,
        CONFIRM,
        NOT_REQUIRED
    }

    public record Source(String label, String url) {}
}
