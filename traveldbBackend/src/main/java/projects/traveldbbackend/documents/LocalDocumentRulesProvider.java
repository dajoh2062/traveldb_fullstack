package projects.traveldbbackend.documents;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import projects.traveldbbackend.api.dto.TravelDocument;
import projects.traveldbbackend.api.dto.TravelDocument.Type;
import projects.traveldbbackend.documents.DocumentRequirement.Scope;
import projects.traveldbbackend.documents.DocumentRequirement.Status;
import projects.traveldbbackend.documents.DocumentRuleSnapshot.Rule;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Component
@Primary
public class LocalDocumentRulesProvider implements DocumentRequirementsProvider {

    private static final String PROVIDER_NAME = "TRAVELDB_LOCAL_RULES";
    private static final String ENTRY_CONDITIONS = "ENTRY_CONDITIONS";
    private static final String DOCUMENT_ACCEPTANCE_PREFIX = "DOCUMENT_ACCEPTANCE_";
    private static final String EU_TRAVEL_DOCUMENT = "EU_EEA_CH_TRAVEL_DOCUMENT";
    private static final Set<String> PERMISSION_FALLBACK_CODES = Set.of(
            "ENTRY_PERMISSION",
            "TRANSIT_PERMISSION"
    );
    private static final Set<String> NON_VISITOR_PURPOSES = Set.of("WORK", "STUDY", "OTHER");

    private final ConservativeDocumentProvider conservativeProvider;
    private final DocumentRuleSnapshot snapshot;

    public LocalDocumentRulesProvider(
            ConservativeDocumentProvider conservativeProvider,
            ObjectMapper objectMapper,
            @Value("${traveldb.documents.rules-location:classpath:data/document-rules.json}") Resource snapshotResource
    ) {
        this.conservativeProvider = conservativeProvider;
        this.snapshot = DocumentRuleSnapshotLoader.load(objectMapper, snapshotResource);
    }

    @Override
    public DocumentCheckResult check(DocumentCheckInput input) {
        DocumentCheckResult conservativeResult = conservativeProvider.check(input);
        LocalDate travelDate = input.departureDate() == null ? LocalDate.now() : input.departureDate();
        List<DocumentRequirement> requirements = new ArrayList<>();
        Set<String> staleRuleIds = new LinkedHashSet<>();

        addJourneyRequirements(input, travelDate, conservativeResult, requirements, staleRuleIds);
        for (DocumentRouteVisitResolver.CountryVisit visit : DocumentRouteVisitResolver.resolve(
                input.route(),
                input.entryAirportCodes()
        )) {
            addVisitRequirements(
                    input,
                    visit,
                    travelDate,
                    conservativeResult,
                    requirements,
                    staleRuleIds
            );
        }

        List<String> warnings = buildWarnings(input, staleRuleIds);
        boolean hasPermissionFallback = requirements.stream()
                .map(DocumentRequirement::code)
                .anyMatch(PERMISSION_FALLBACK_CODES::contains);
        boolean hasRestrictedPrimaryDocument = hasRegisteredDocuments(input)
                && !hasOrdinaryPassportPrimary(input);

        return new DocumentCheckResult(
                PROVIDER_NAME,
                false,
                hasPermissionFallback || hasRestrictedPrimaryDocument
                        ? "LOCAL_RULES_WITH_VERIFY_FALLBACK"
                        : "LOCAL_VERSIONED_RULE_SNAPSHOT",
                Instant.now(),
                List.copyOf(requirements),
                conservativeResult.missingInputs(),
                warnings,
                snapshot.sources()
        );
    }

    private void addJourneyRequirements(
            DocumentCheckInput input,
            LocalDate travelDate,
            DocumentCheckResult conservativeResult,
            List<DocumentRequirement> requirements,
            Set<String> staleRuleIds
    ) {
        List<Rule> rules = selectRules(input, null, Scope.JOURNEY, travelDate);
        if (rules.isEmpty()) {
            conservativeResult.requirements().stream()
                    .filter(requirement -> requirement.scope() == Scope.JOURNEY)
                    .forEach(requirements::add);
            return;
        }

        rules.stream()
                .map(rule -> toRequirement(rule, null, null, staleRuleIds))
                .forEach(requirements::add);
        conservativeResult.requirements().stream()
                .filter(requirement -> requirement.code().startsWith(DOCUMENT_ACCEPTANCE_PREFIX))
                .forEach(requirements::add);
    }

    private void addVisitRequirements(
            DocumentCheckInput input,
            DocumentRouteVisitResolver.CountryVisit visit,
            LocalDate travelDate,
            DocumentCheckResult conservativeResult,
            List<DocumentRequirement> requirements,
            Set<String> staleRuleIds
    ) {
        List<Rule> rules = visit.scope() == Scope.ENTRY && hasNonVisitorPurpose(input.travelPurpose())
                ? List.of()
                : selectRules(input, visit.countryCode(), visit.scope(), travelDate);

        if (rules.isEmpty()) {
            conservativeRequirementsForVisit(conservativeResult, visit, false).forEach(requirements::add);
        } else {
            rules.stream()
                    .map(rule -> toRequirement(rule, visit.countryCode(), visit.airportCode(), staleRuleIds))
                    .forEach(requirements::add);
        }

        if (visit.scope() == Scope.ENTRY) {
            conservativeRequirementsForVisit(conservativeResult, visit, true).forEach(requirements::add);
        }
    }

    private List<DocumentRequirement> conservativeRequirementsForVisit(
            DocumentCheckResult conservativeResult,
            DocumentRouteVisitResolver.CountryVisit visit,
            boolean entryConditionsOnly
    ) {
        return conservativeResult.requirements().stream()
                .filter(requirement -> requirement.scope() == visit.scope())
                .filter(requirement -> visit.countryCode().equals(requirement.countryCode()))
                .filter(requirement -> visit.airportCode().equals(requirement.airportCode()))
                .filter(requirement -> entryConditionsOnly == ENTRY_CONDITIONS.equals(requirement.code()))
                .toList();
    }

    private List<Rule> selectRules(
            DocumentCheckInput input,
            String destinationCountryCode,
            Scope scope,
            LocalDate travelDate
    ) {
        boolean restrictNationalityRules = hasRegisteredDocuments(input)
                && !hasOrdinaryPassportPrimary(input);
        String documentCountryCode = input.passportIssuingCountryCode() == null
                ? input.nationalityCountryCode()
                : input.passportIssuingCountryCode();
        Map<String, Rule> decisions = new LinkedHashMap<>();

        snapshot.rules().stream()
                .filter(rule -> rule.scope() == scope)
                .filter(rule -> !restrictNationalityRules
                        || isNationalIdPrimary(input) && EU_TRAVEL_DOCUMENT.equals(rule.code()))
                .filter(rule -> matches(rule.destinationCountries(), destinationCountryCode))
                .filter(rule -> matches(rule.nationalities(), documentCountryCode))
                .filter(rule -> !contains(rule.excludedNationalities(), documentCountryCode))
                .filter(rule -> matches(rule.residenceCountries(), input.residenceCountryCode()))
                .filter(rule -> matches(rule.passportIssuingCountries(), input.passportIssuingCountryCode()))
                .filter(rule -> matches(rule.travelPurposes(), normalize(input.travelPurpose())))
                .filter(rule -> matchesAge(rule, input.travelerAge()))
                .filter(rule -> containsAll(input.visaCountryCodes(), rule.requiredHeldVisaCountries()))
                .filter(rule -> containsAll(input.residencePermitCountryCodes(), rule.requiredResidencePermitCountries()))
                .filter(rule -> rule.effectiveFrom() == null || !travelDate.isBefore(rule.effectiveFrom()))
                .filter(rule -> rule.effectiveTo() == null || !travelDate.isAfter(rule.effectiveTo()))
                .sorted(Comparator.comparingInt(Rule::priority).reversed().thenComparing(Rule::id))
                .forEach(rule -> decisions.putIfAbsent(rule.decisionKey(), rule));
        return List.copyOf(decisions.values());
    }

    private DocumentRequirement toRequirement(
            Rule rule,
            String countryCode,
            String airportCode,
            Set<String> staleRuleIds
    ) {
        boolean stale = rule.reviewAfter() != null && rule.reviewAfter().isBefore(LocalDate.now());
        Status status = stale && rule.status() != Status.VERIFY ? Status.VERIFY : rule.status();
        List<String> conditions = new ArrayList<>(rule.conditions());
        conditions.add("Local rule " + rule.id() + " was last verified " + rule.lastVerified() + ".");
        if (stale) {
            staleRuleIds.add(rule.id());
        }

        String summary = stale
                ? "This stored rule needs review before it can be treated as definitive. " + rule.summary()
                : rule.summary();
        return new DocumentRequirement(
                rule.code(),
                rule.category(),
                status,
                rule.scope(),
                countryCode,
                airportCode,
                rule.title(),
                summary,
                List.copyOf(conditions),
                rule.sources()
        );
    }

    private List<String> buildWarnings(DocumentCheckInput input, Set<String> staleRuleIds) {
        List<String> warnings = new ArrayList<>();
        warnings.add("TravelDB evaluated this journey locally from rule snapshot " + snapshot.datasetVersion()
                + "; no external requirements service was contacted.");
        if (!staleRuleIds.isEmpty()) {
            warnings.add("Some local rules are past their review date and were downgraded to verification-only: "
                    + String.join(", ", staleRuleIds) + ".");
        }
        if (input.departureDate() == null) {
            warnings.add("No departure date was supplied; effective-date matching used today's date.");
        }
        if (hasRegisteredDocuments(input) && !hasOrdinaryPassportPrimary(input)) {
            warnings.add(isNationalIdPrimary(input)
                    ? "Only reviewed EU, EEA and Swiss identity-card rules were considered for the selected national identity card; other route decisions were limited to verification guidance."
                    : "The selected primary document is not an ordinary passport; nationality-based passport rules were limited to verification guidance.");
        }
        return List.copyOf(warnings);
    }

    private static boolean hasRegisteredDocuments(DocumentCheckInput input) {
        return input.travelDocuments() != null && !input.travelDocuments().isEmpty();
    }

    private static boolean hasOrdinaryPassportPrimary(DocumentCheckInput input) {
        return primaryDocumentType(input) == Type.PASSPORT;
    }

    private static boolean isNationalIdPrimary(DocumentCheckInput input) {
        return primaryDocumentType(input) == Type.NATIONAL_ID_CARD;
    }

    private static Type primaryDocumentType(DocumentCheckInput input) {
        if (input.travelDocuments() == null) {
            return null;
        }
        return input.travelDocuments().stream()
                .filter(document -> Boolean.TRUE.equals(document.primary()))
                .map(TravelDocument::type)
                .map(LocalDocumentRulesProvider::parseDocumentType)
                .filter(type -> type != null)
                .findFirst()
                .orElse(null);
    }

    private static Type parseDocumentType(String value) {
        try {
            return Type.valueOf(value);
        } catch (IllegalArgumentException | NullPointerException ignored) {
            return null;
        }
    }

    private static boolean matches(List<String> allowed, String value) {
        return allowed.isEmpty()
                || allowed.contains("*")
                || value != null && allowed.contains(normalize(value));
    }

    private static boolean contains(List<String> values, String value) {
        return value != null && values.contains(normalize(value));
    }

    private static boolean containsAll(List<String> held, List<String> required) {
        if (required.isEmpty()) {
            return true;
        }

        Set<String> normalizedHeld = new LinkedHashSet<>();
        if (held != null) {
            held.forEach(value -> normalizedHeld.add(normalize(value)));
        }
        return normalizedHeld.containsAll(required);
    }

    private static boolean matchesAge(Rule rule, Integer age) {
        if (rule.minimumAge() == null && rule.maximumAge() == null) {
            return true;
        }
        if (age == null) {
            return false;
        }
        return (rule.minimumAge() == null || age >= rule.minimumAge())
                && (rule.maximumAge() == null || age <= rule.maximumAge());
    }

    private static boolean hasNonVisitorPurpose(String travelPurpose) {
        String purpose = normalize(travelPurpose);
        return purpose != null && NON_VISITOR_PURPOSES.contains(purpose);
    }

    private static String normalize(String value) {
        return value == null ? null : value.trim().toUpperCase(Locale.ROOT);
    }
}
