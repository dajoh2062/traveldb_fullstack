package projects.traveldbbackend.documents;

import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import projects.traveldbbackend.documents.DocumentRequirement.Category;
import projects.traveldbbackend.documents.DocumentRequirement.DocumentSource;
import projects.traveldbbackend.documents.DocumentRequirement.KeyFact;
import projects.traveldbbackend.documents.DocumentRequirement.Scope;
import projects.traveldbbackend.documents.DocumentRequirement.Status;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

final class DocumentRuleSnapshotLoader {

    private static final int SUPPORTED_SCHEMA_VERSION = 2;

    private DocumentRuleSnapshotLoader() {}

    static DocumentRuleSnapshot load(ObjectMapper objectMapper, Resource resource) {
        rejectRemoteRuntimeResource(resource);
        try (var input = resource.getInputStream()) {
            JsonNode root = objectMapper.readTree(input);
            validateSchemaVersion(root);

            String datasetVersion = requiredText(root, "datasetVersion");
            Instant.parse(requiredText(root, "generatedAt"));
            List<DocumentSource> sources = parseSources(root.path("sources"));
            List<DocumentRuleSnapshot.Rule> rules = parseRules(root.path("rules"));
            return new DocumentRuleSnapshot(datasetVersion, sources, rules);
        } catch (IOException | RuntimeException error) {
            throw new IllegalStateException("Could not load local document-rule snapshot from " + resource, error);
        }
    }

    private static void rejectRemoteRuntimeResource(Resource resource) {
        if (!(resource instanceof UrlResource urlResource)) {
            return;
        }
        String protocol = urlResource.getURL().getProtocol();
        if (!"file".equalsIgnoreCase(protocol)) {
            throw new IllegalStateException(
                    "Runtime document rules must come from a bundled classpath or local file resource."
            );
        }
    }

    private static void validateSchemaVersion(JsonNode root) {
        if (root.path("schemaVersion").asInt() != SUPPORTED_SCHEMA_VERSION) {
            throw new IllegalStateException("Unsupported document-rule schema version.");
        }
    }

    private static List<DocumentRuleSnapshot.Rule> parseRules(JsonNode rulesNode) {
        List<DocumentRuleSnapshot.Rule> rules = new ArrayList<>();
        Set<String> ids = new LinkedHashSet<>();
        for (JsonNode ruleNode : rulesNode) {
            DocumentRuleSnapshot.Rule rule = parseRule(ruleNode);
            if (!ids.add(rule.id())) {
                throw new IllegalStateException("Duplicate document rule id: " + rule.id());
            }
            rules.add(rule);
        }
        if (rules.isEmpty()) {
            throw new IllegalStateException("Document-rule snapshot contains no rules.");
        }
        return List.copyOf(rules);
    }

    private static DocumentRuleSnapshot.Rule parseRule(JsonNode node) {
        JsonNode output = node.path("output");
        return new DocumentRuleSnapshot.Rule(
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
                parseSources(output.path("sources")),
                parseKeyFacts(output.path("keyFacts"))
        );
    }

    private static List<KeyFact> parseKeyFacts(JsonNode node) {
        if (!node.isArray()) {
            return List.of();
        }

        List<KeyFact> keyFacts = new ArrayList<>();
        node.forEach(fact -> keyFacts.add(new KeyFact(
                requiredText(fact, "label"),
                requiredText(fact, "value")
        )));
        return List.copyOf(keyFacts);
    }

    private static List<DocumentSource> parseSources(JsonNode node) {
        if (!node.isArray()) {
            return List.of();
        }

        List<DocumentSource> sources = new ArrayList<>();
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

    private static List<String> tokenList(JsonNode node) {
        if (!node.isArray()) {
            return List.of();
        }

        List<String> values = new ArrayList<>();
        node.forEach(value -> {
            if (value.isString() && !value.asString().isBlank()) {
                values.add(normalize(value.asString()));
            }
        });
        return List.copyOf(values);
    }

    private static List<String> stringList(JsonNode node) {
        if (!node.isArray()) {
            return List.of();
        }

        List<String> values = new ArrayList<>();
        node.forEach(value -> {
            if (value.isString() && !value.asString().isBlank()) {
                values.add(value.asString().trim());
            }
        });
        return List.copyOf(values);
    }

    private static String requiredText(JsonNode node, String field) {
        String value = node.path(field).asString().trim();
        if (value.isBlank()) {
            throw new IllegalStateException("Missing document-rule field: " + field);
        }
        return value;
    }

    private static String requiredHttpsUrl(JsonNode node, String field) {
        String value = requiredText(node, field);
        if (!value.startsWith("https://")) {
            throw new IllegalStateException("Rule sources must use HTTPS: " + value);
        }
        return value;
    }

    private static Integer nullableInt(JsonNode node) {
        return node.isIntegralNumber() ? node.asInt() : null;
    }

    private static LocalDate nullableDate(JsonNode node) {
        return node.isString() && !node.asString().isBlank()
                ? LocalDate.parse(node.asString())
                : null;
    }

    private static String normalize(String value) {
        return value.trim().toUpperCase(Locale.ROOT);
    }
}
