package projects.traveldbbackend.documents;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import projects.traveldbbackend.Airport;
import projects.traveldbbackend.documents.DocumentRequirement.Category;
import projects.traveldbbackend.documents.DocumentRequirement.DocumentSource;
import projects.traveldbbackend.documents.DocumentRequirement.Scope;
import projects.traveldbbackend.documents.DocumentRequirement.Status;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
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
public class LocalDocumentRulesProvider implements DocumentRequirementsProvider {

    private final ConservativeDocumentProvider conservativeProvider;
    private final Snapshot snapshot;

    public LocalDocumentRulesProvider(
            ConservativeDocumentProvider conservativeProvider,
            ObjectMapper objectMapper,
            @Value("${traveldb.documents.rules-location:classpath:data/document-rules.json}") Resource snapshotResource
    ) {
        this.conservativeProvider = conservativeProvider;
        this.snapshot = loadSnapshot(objectMapper, snapshotResource);
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public DocumentCheckResult check(DocumentCheckInput input) {
        DocumentCheckResult conservative = conservativeProvider.check(input);
        LocalDate travelDate = input.departureDate() == null ? LocalDate.now() : input.departureDate();
        List<DocumentRequirement> requirements = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        Set<String> staleRuleIds = new LinkedHashSet<>();

        List<Rule> journeyRules = selectRules(input, null, Scope.JOURNEY, travelDate);
        if (journeyRules.isEmpty()) {
            requirements.addAll(conservative.requirements().stream()
                    .filter(requirement -> requirement.scope() == Scope.JOURNEY)
                    .toList());
        } else {
            journeyRules.forEach(rule -> requirements.add(toRequirement(rule, null, staleRuleIds)));
        }

        for (CountryVisit visit : countryVisits(input.route())) {
            List<Rule> visitRules = selectRules(input, visit.countryCode(), visit.scope(), travelDate);
            if (visitRules.isEmpty()) {
                requirements.addAll(conservative.requirements().stream()
                        .filter(requirement -> requirement.scope() == visit.scope()
                                && visit.countryCode().equals(requirement.countryCode())
                                && visit.airportCode().equals(requirement.airportCode())
                                && !"ENTRY_CONDITIONS".equals(requirement.code()))
                        .toList());
            } else {
                visitRules.forEach(rule -> requirements.add(toRequirement(rule, visit.airportCode(), staleRuleIds)));
            }

            if (visit.scope() == Scope.ENTRY) {
                requirements.addAll(conservative.requirements().stream()
                        .filter(requirement -> "ENTRY_CONDITIONS".equals(requirement.code())
                                && visit.countryCode().equals(requirement.countryCode())
                                && visit.airportCode().equals(requirement.airportCode()))
                        .toList());
            }
        }

        warnings.add("TravelDB evaluated this journey locally from rule snapshot " + snapshot.datasetVersion()
                + "; no external requirements service was contacted.");
        if (!staleRuleIds.isEmpty()) {
            warnings.add("Some local rules are past their review date and were downgraded to verification-only: "
                    + String.join(", ", staleRuleIds) + ".");
        }
        if (input.departureDate() == null) {
            warnings.add("No departure date was supplied; effective-date matching used today's date.");
        }

        return new DocumentCheckResult(
                "TRAVELDB_LOCAL_RULES",
                false,
                "LOCAL_VERSIONED_RULE_SNAPSHOT",
                Instant.now(),
                List.copyOf(requirements),
                conservative.missingInputs(),
                List.copyOf(warnings),
                snapshot.sources()
        );
    }

    public String datasetVersion() {
        return snapshot.datasetVersion();
    }

    private List<Rule> selectRules(
            DocumentCheckInput input,
            String destinationCountryCode,
            Scope scope,
            LocalDate travelDate
    ) {
        Map<String, Rule> decisions = new LinkedHashMap<>();
        snapshot.rules().stream()
                .filter(rule -> rule.scope() == scope)
                .filter(rule -> matches(rule.destinationCountries(), destinationCountryCode))
                .filter(rule -> matches(rule.nationalities(), input.nationalityCountryCode()))
                .filter(rule -> !contains(rule.excludedNationalities(), input.nationalityCountryCode()))
                .filter(rule -> matchesOptional(rule.residenceCountries(), input.residenceCountryCode()))
                .filter(rule -> matchesOptional(rule.passportIssuingCountries(), input.passportIssuingCountryCode()))
                .filter(rule -> matchesOptional(rule.travelPurposes(), normalize(input.travelPurpose())))
                .filter(rule -> matchesAge(rule, input.travelerAge()))
                .filter(rule -> containsAll(input.visaCountryCodes(), rule.requiredHeldVisaCountries()))
                .filter(rule -> containsAll(input.residencePermitCountryCodes(), rule.requiredResidencePermitCountries()))
                .filter(rule -> rule.effectiveFrom() == null || !travelDate.isBefore(rule.effectiveFrom()))
                .filter(rule -> rule.effectiveTo() == null || !travelDate.isAfter(rule.effectiveTo()))
                .sorted(Comparator.comparingInt(Rule::priority).reversed().thenComparing(Rule::id))
                .forEach(rule -> decisions.putIfAbsent(rule.decisionKey(), rule));
        return List.copyOf(decisions.values());
    }

    private DocumentRequirement toRequirement(Rule rule, String airportCode, Set<String> staleRuleIds) {
        boolean stale = rule.reviewAfter() != null && rule.reviewAfter().isBefore(LocalDate.now());
        Status status = stale && (rule.status() == Status.REQUIRED || rule.status() == Status.NOT_REQUIRED)
                ? Status.VERIFY
                : rule.status();
        List<String> conditions = new ArrayList<>(rule.conditions());
        conditions.add("Local rule " + rule.id() + " was last verified " + rule.lastVerified() + ".");
        if (stale) staleRuleIds.add(rule.id());

        return new DocumentRequirement(
                rule.code(),
                rule.category(),
                status,
                rule.scope(),
                firstSpecificCountry(rule.destinationCountries()),
                airportCode,
                rule.title(),
                stale ? "This stored rule needs review before it can be treated as definitive. " + rule.summary() : rule.summary(),
                List.copyOf(conditions),
                rule.sources()
        );
    }

    private boolean matches(List<String> allowed, String value) {
        if (allowed.isEmpty() || allowed.contains("*")) return true;
        return value != null && allowed.contains(normalize(value));
    }

    private boolean matchesOptional(List<String> allowed, String value) {
        return allowed.isEmpty() || allowed.contains("*") || (value != null && allowed.contains(normalize(value)));
    }

    private boolean contains(List<String> values, String value) {
        return value != null && values.contains(normalize(value));
    }

    private boolean containsAll(List<String> held, List<String> required) {
        if (required.isEmpty()) return true;
        Set<String> normalizedHeld = new LinkedHashSet<>();
        if (held != null) held.forEach(value -> normalizedHeld.add(normalize(value)));
        return normalizedHeld.containsAll(required);
    }

    private boolean matchesAge(Rule rule, Integer age) {
        if (rule.minimumAge() == null && rule.maximumAge() == null) return true;
        if (age == null) return false;
        return (rule.minimumAge() == null || age >= rule.minimumAge())
                && (rule.maximumAge() == null || age <= rule.maximumAge());
    }

    private String firstSpecificCountry(List<String> countries) {
        return countries.stream().filter(country -> !"*".equals(country)).findFirst().orElse(null);
    }

    private List<CountryVisit> countryVisits(List<Airport> route) {
        List<CountryVisit> visits = new ArrayList<>();
        for (int index = 1; index < route.size(); index++) {
            Airport airport = route.get(index);
            Scope scope = index == route.size() - 1 ? Scope.ENTRY : Scope.TRANSIT;
            visits.add(new CountryVisit(airport.getCountryCode(), airport.getIataCode(), scope));
        }
        return visits;
    }

    private Snapshot loadSnapshot(ObjectMapper objectMapper, Resource resource) {
        try (var input = resource.getInputStream()) {
            JsonNode root = objectMapper.readTree(input);
            if (root.path("schemaVersion").asInt() != 1) {
                throw new IllegalStateException("Unsupported document-rule schema version.");
            }
            String datasetVersion = requiredText(root, "datasetVersion");
            Instant generatedAt = Instant.parse(requiredText(root, "generatedAt"));
            List<DocumentSource> sources = parseSources(root.path("sources"));
            List<Rule> rules = new ArrayList<>();
            Set<String> ids = new LinkedHashSet<>();
            for (JsonNode node : root.path("rules")) {
                Rule rule = parseRule(node);
                if (!ids.add(rule.id())) throw new IllegalStateException("Duplicate document rule id: " + rule.id());
                rules.add(rule);
            }
            if (rules.isEmpty()) throw new IllegalStateException("Document-rule snapshot contains no rules.");
            return new Snapshot(datasetVersion, generatedAt, List.copyOf(sources), List.copyOf(rules));
        } catch (IOException | RuntimeException error) {
            throw new IllegalStateException("Could not load local document-rule snapshot from " + resource, error);
        }
    }

    private Rule parseRule(JsonNode node) {
        JsonNode output = node.path("output");
        return new Rule(
                requiredText(node, "id"),
                requiredText(node, "decisionKey"),
                Scope.valueOf(requiredText(node, "scope")),
                tokenList(node.path("destinationCountries")),
                tokenList(node.path("nationalities")),
                tokenList(node.path("excludedNationalities")),
                tokenList(node.path("residenceCountries")),
                tokenList(node.path("passportIssuingCountries")),
                tokenList(node.path("travelPurposes")),
                nullableInt(node.path("minimumAge")),
                nullableInt(node.path("maximumAge")),
                tokenList(node.path("requiredHeldVisaCountries")),
                tokenList(node.path("requiredResidencePermitCountries")),
                node.path("priority").asInt(),
                nullableDate(node.path("effectiveFrom")),
                nullableDate(node.path("effectiveTo")),
                nullableDate(node.path("lastVerified")),
                nullableDate(node.path("reviewAfter")),
                requiredText(output, "code"),
                Category.valueOf(requiredText(output, "category")),
                Status.valueOf(requiredText(output, "status")),
                requiredText(output, "title"),
                requiredText(output, "summary"),
                stringList(output.path("conditions")),
                parseSources(output.path("sources"))
        );
    }

    private List<DocumentSource> parseSources(JsonNode node) {
        List<DocumentSource> sources = new ArrayList<>();
        if (!node.isArray()) return sources;
        node.forEach(source -> {
            String sourceType = requiredText(source, "sourceType");
            if (!"GOVERNMENT".equals(sourceType)) {
                throw new IllegalStateException("Local document rules must cite government sources.");
            }
            sources.add(new DocumentSource(
                    requiredText(source, "label"),
                    requiredHttpsUrl(source, "url"),
                    sourceType
            ));
        });
        return List.copyOf(sources);
    }

    private List<String> tokenList(JsonNode node) {
        if (!node.isArray()) return List.of();
        List<String> values = new ArrayList<>();
        node.forEach(value -> {
            if (value.isTextual() && !value.asText().isBlank()) values.add(normalize(value.asText()));
        });
        return List.copyOf(values);
    }

    private List<String> stringList(JsonNode node) {
        if (!node.isArray()) return List.of();
        List<String> values = new ArrayList<>();
        node.forEach(value -> {
            if (value.isTextual() && !value.asText().isBlank()) values.add(value.asText().trim());
        });
        return List.copyOf(values);
    }

    private String requiredText(JsonNode node, String field) {
        String value = node.path(field).asText().trim();
        if (value.isBlank()) throw new IllegalStateException("Missing document-rule field: " + field);
        return value;
    }

    private String requiredHttpsUrl(JsonNode node, String field) {
        String value = requiredText(node, field);
        if (!value.startsWith("https://")) throw new IllegalStateException("Rule sources must use HTTPS: " + value);
        return value;
    }

    private Integer nullableInt(JsonNode node) {
        return node.isIntegralNumber() ? node.asInt() : null;
    }

    private LocalDate nullableDate(JsonNode node) {
        return node.isTextual() && !node.asText().isBlank() ? LocalDate.parse(node.asText()) : null;
    }

    private String normalize(String value) {
        return value == null ? null : value.trim().toUpperCase(Locale.ROOT);
    }

    private record Snapshot(
            String datasetVersion,
            Instant generatedAt,
            List<DocumentSource> sources,
            List<Rule> rules
    ) {}

    private record Rule(
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

    private record CountryVisit(String countryCode, String airportCode, Scope scope) {}
}
