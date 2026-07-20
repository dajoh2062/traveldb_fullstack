package projects.traveldbbackend.documents;

import java.time.Instant;
import java.util.List;

public record DocumentCheckResult(
        String provider,
        boolean liveData,
        String coverage,
        Instant checkedAt,
        List<DocumentRequirement> requirements,
        List<String> missingInputs,
        List<String> warnings,
        List<DocumentRequirement.DocumentSource> verificationSources
) {
    public static DocumentCheckResult unavailable(String warning) {
        return new DocumentCheckResult(
                "TRAVELDB_CONSERVATIVE",
                false,
                "VERIFY_WITH_AUTHORITATIVE_PROVIDER",
                Instant.now(),
                List.of(),
                List.of(),
                List.of(warning),
                List.of()
        );
    }
}
