package io.github.dajoh2062.traveldb.baggage;

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
