package projects.traveldbbackend.documents;

import projects.traveldbbackend.documents.DocumentRequirement.Category;
import projects.traveldbbackend.documents.DocumentRequirement.DocumentSource;
import projects.traveldbbackend.documents.DocumentRequirement.Scope;
import projects.traveldbbackend.documents.DocumentRequirement.Status;

import java.time.LocalDate;
import java.util.List;

record DocumentRuleSnapshot(
        String datasetVersion,
        List<DocumentSource> sources,
        List<Rule> rules
) {
    record Rule(
            String id,
            String decisionKey,
            Scope scope,
            List<String> destinationCountries,
            List<String> nationalities,
            List<String> excludedNationalities,
            List<String> residenceCountries,
            List<String> passportIssuingCountries,
            List<String> travelPurposes,
            Integer minimumAge,
            Integer maximumAge,
            List<String> requiredHeldVisaCountries,
            List<String> requiredResidencePermitCountries,
            int priority,
            LocalDate effectiveFrom,
            LocalDate effectiveTo,
            LocalDate lastVerified,
            LocalDate reviewAfter,
            String code,
            Category category,
            Status status,
            String title,
            String summary,
            List<String> conditions,
            List<DocumentSource> sources
    ) {}
}
