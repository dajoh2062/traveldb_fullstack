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
        List<DocumentRequirement.DocumentSource> verificationSources,
        String datasetVersion
) {
}
