package io.github.dajoh2062.traveldb.baggage;

import java.util.List;

public record BaggageAdvice(
        String airportCode,
        Status status,
        String title,
        String explanation,
        List<String> exceptions,
        List<Source> sources,
        AdviceCode adviceCode
) {
    public BaggageAdvice(
            String airportCode,
            Status status,
            String title,
            String explanation,
            List<String> exceptions,
            List<Source> sources
    ) {
        this(airportCode, status, title, explanation, exceptions, sources, null);
    }

    public enum AdviceCode {
        DELHI_INTERNATIONAL_TO_DOMESTIC,
        AUSTRALIA_INTERNATIONAL_TO_DOMESTIC,
        NEW_ZEALAND_INTERNATIONAL_TO_DOMESTIC,
        JAPAN_INTERNATIONAL_TO_DOMESTIC,
        CANADA_INTERNATIONAL_TO_DOMESTIC,
        GENERIC_INTERNATIONAL_TO_DOMESTIC,
        NOT_CHECKED_THROUGH,
        CHECKED_THROUGH_NO_KNOWN_RECLAIM,
        SEPARATE_TICKETS,
        CHECK_BAGGAGE_TAG,
        US_PRECLEARANCE_NOT_CHECKED_THROUGH,
        US_PRECLEARANCE_CHECKED_THROUGH,
        US_PRECLEARANCE_CONFIRM_TAG,
        US_SYD_LAX_SCREENING_PILOT,
        US_FIRST_ARRIVAL
    }

    public enum Status {
        REQUIRED,
        CONFIRM,
        NOT_REQUIRED
    }

    public record Source(String label, String url) {}
}
