package projects.traveldbbackend.documents;

import java.time.LocalDate;
import java.util.List;

public record DocumentRequirement(
        String code,
        Category category,
        Status status,
        Scope scope,
        String countryCode,
        String airportCode,
        String title,
        String summary,
        List<String> conditions,
        List<DocumentSource> sources,
        List<KeyFact> keyFacts,
        LocalDate lastVerified,
        LocalDate reviewAfter
) {
    public DocumentRequirement(
            String code,
            Category category,
            Status status,
            Scope scope,
            String countryCode,
            String airportCode,
            String title,
            String summary,
            List<String> conditions,
            List<DocumentSource> sources
    ) {
        this(code, category, status, scope, countryCode, airportCode, title, summary,
                conditions, sources, List.of(), null, null);
    }

    public enum Category {
        TRAVEL_DOCUMENT,
        PASSPORT_VALIDITY,
        VISA,
        ELECTRONIC_AUTHORIZATION,
        TRANSIT_PERMISSION,
        HEALTH,
        ARRIVAL_FORM,
        ONWARD_TRAVEL,
        OTHER
    }

    public enum Status {
        REQUIRED,
        NOT_REQUIRED,
        CONDITIONAL,
        VERIFY
    }

    public enum Scope {
        JOURNEY,
        ENTRY,
        TRANSIT
    }

    public record KeyFact(String label, String value) {}

    public record DocumentSource(String label, String url, String sourceType) {}
}
