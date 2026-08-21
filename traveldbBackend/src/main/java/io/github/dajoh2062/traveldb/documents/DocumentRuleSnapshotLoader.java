package io.github.dajoh2062.traveldb.documents;

import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import io.github.dajoh2062.traveldb.documents.DocumentRequirement.Category;
import io.github.dajoh2062.traveldb.documents.DocumentRequirement.DocumentSource;
import io.github.dajoh2062.traveldb.documents.DocumentRequirement.KeyFact;
import io.github.dajoh2062.traveldb.documents.DocumentRequirement.Scope;
import io.github.dajoh2062.traveldb.documents.DocumentRequirement.Status;
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
import java.util.regex.Pattern;

final class DocumentRuleSnapshotLoader {

    private static final int SUPPORTED_SCHEMA_VERSION = 2;
    private static final Pattern COUNTRY_CODE = Pattern.compile("[A-Z]{2}");
    private static final Pattern TOKEN = Pattern.compile("[A-Z0-9_]+");

    private DocumentRuleSnapshotLoader() {}

    static DocumentRuleSnapshot load(ObjectMapper objectMapper, Resource resource) {
        rejectRemoteRuntimeResource(resource);
        try (var input = resource.getInputStream()) {
            JsonNode root = objectMapper.readTree(input);
            validateSchemaVersion(root);

            String datasetVersion = requiredText(root, "datasetVersion");
            Instant generatedAt = Instant.parse(requiredText(root, "generatedAt"));
            List<DocumentSource> sources = parseSources(root.path("sources"));
            List<DocumentRule> rules = parseRules(root.path("rules"));
            return new DocumentRuleSnapshot(datasetVersion, generatedAt, sources, rules);
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
        JsonNode schemaVersion = root.get("schemaVersion");
        if (schemaVersion == null
                || !schemaVersion.isIntegralNumber()
                || !schemaVersion.canConvertToInt()
                || schemaVersion.asInt() != SUPPORTED_SCHEMA_VERSION) {
            throw new IllegalStateException("Unsupported document-rule schema version.");
        }
    }

    private static List<DocumentRule> parseRules(JsonNode rulesNode) {
        if (!rulesNode.isArray()) {
            throw new IllegalStateException("Document-rule snapshot rules must be an array.");
        }
        List<DocumentRule> rules = new ArrayList<>();
        Set<String> ids = new LinkedHashSet<>();
        for (JsonNode ruleNode : rulesNode) {
            DocumentRule rule = parseRule(ruleNode);
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

    private static DocumentRule parseRule(JsonNode node) {
        JsonNode output = node.path("output");
        Integer minimumAge = optionalNonNegativeInt(node, "minimumAge");
        Integer maximumAge = optionalNonNegativeInt(node, "maximumAge");
        if (minimumAge != null && maximumAge != null && minimumAge > maximumAge) {
            throw new IllegalStateException("Document-rule minimumAge must not exceed maximumAge.");
        }
        return new DocumentRule(
                requiredText(node, "id"),
                requiredText(node, "decisionKey"),
                Scope.valueOf(requiredText(node, "scope")),
                requiredCountrySelector(node, "destinationCountries", true),
                requiredCountrySelector(node, "nationalities", true),
                optionalCountrySelector(node, "excludedNationalities", false),
                optionalCountrySelector(node, "residenceCountries", true),
                optionalCountrySelector(node, "passportIssuingCountries", true),
                optionalTokenSelector(node, "travelPurposes", true),
                minimumAge,
                maximumAge,
                optionalCountrySelector(node, "requiredHeldVisaCountries", false),
                optionalCountrySelector(node, "requiredResidencePermitCountries", false),
                requiredNonNegativeInt(node, "priority"),
                optionalDate(node, "effectiveFrom"),
                optionalDate(node, "effectiveTo"),
                requiredDate(node, "lastVerified"),
                requiredDate(node, "reviewAfter"),
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
        if (node.isMissingNode()) {
            return List.of();
        }
        if (!node.isArray()) {
            throw new IllegalStateException("Document-rule keyFacts must be an array.");
        }
        if (node.size() > 6) {
            throw new IllegalStateException("Document-rule keyFacts must contain at most six items.");
        }

        List<KeyFact> keyFacts = new ArrayList<>();
        Set<String> labels = new LinkedHashSet<>();
        node.forEach(fact -> {
            String label = requiredText(fact, "label");
            if (!labels.add(label.toUpperCase(Locale.ROOT))) {
                throw new IllegalStateException("Document-rule keyFacts labels must be unique.");
            }
            keyFacts.add(new KeyFact(label, requiredText(fact, "value")));
        });
        return List.copyOf(keyFacts);
    }

    private static List<DocumentSource> parseSources(JsonNode node) {
        if (!node.isArray()) {
            throw new IllegalStateException("Document-rule sources must be an array.");
        }
        if (node.isEmpty()) {
            throw new IllegalStateException("Document-rule sources must not be empty.");
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

    private static List<String> requiredCountrySelector(
            JsonNode rule,
            String field,
            boolean allowWildcard
    ) {
        return selectorList(rule, field, true, true, allowWildcard);
    }

    private static List<String> optionalCountrySelector(
            JsonNode rule,
            String field,
            boolean allowWildcard
    ) {
        return selectorList(rule, field, false, true, allowWildcard);
    }

    private static List<String> optionalTokenSelector(
            JsonNode rule,
            String field,
            boolean allowWildcard
    ) {
        return selectorList(rule, field, false, false, allowWildcard);
    }

    private static List<String> selectorList(
            JsonNode rule,
            String field,
            boolean required,
            boolean countryCodes,
            boolean allowWildcard
    ) {
        JsonNode node = rule.get(field);
        if (node == null) {
            if (required) {
                throw new IllegalStateException("Missing document-rule selector: " + field);
            }
            return List.of();
        }
        if (!node.isArray()) {
            throw new IllegalStateException("Document-rule selector must be an array: " + field);
        }
        if (required && node.size() == 0) {
            throw new IllegalStateException("Document-rule selector must not be empty: " + field);
        }

        List<String> values = new ArrayList<>();
        Set<String> seen = new LinkedHashSet<>();
        for (JsonNode valueNode : node) {
            if (!valueNode.isString()) {
                throw new IllegalStateException("Invalid document-rule selector value: " + field);
            }

            String value = valueNode.asString();
            boolean valid = (allowWildcard && "*".equals(value))
                    || (countryCodes && COUNTRY_CODE.matcher(value).matches())
                    || (!countryCodes && TOKEN.matcher(value).matches());
            if (!valid) {
                throw new IllegalStateException("Invalid document-rule selector value for " + field + ": " + value);
            }
            if (!seen.add(value)) {
                throw new IllegalStateException("Duplicate document-rule selector value for " + field + ": " + value);
            }
            values.add(value);
        }
        if (values.size() > 1 && values.contains("*")) {
            throw new IllegalStateException("Document-rule selector cannot combine * with specific values: " + field);
        }
        return List.copyOf(values);
    }

    private static List<String> stringList(JsonNode node) {
        if (!node.isArray()) {
            throw new IllegalStateException("Document-rule conditions must be an array.");
        }

        List<String> values = new ArrayList<>();
        for (JsonNode value : node) {
            if (!value.isString() || value.asString().isBlank()) {
                throw new IllegalStateException("Document-rule conditions must contain non-empty strings.");
            }
            values.add(value.asString().trim());
        }
        return List.copyOf(values);
    }

    private static String requiredText(JsonNode node, String field) {
        JsonNode valueNode = node.get(field);
        if (valueNode == null || !valueNode.isString()) {
            throw new IllegalStateException("Missing document-rule field: " + field);
        }
        String value = valueNode.asString().trim();
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

    private static int requiredNonNegativeInt(JsonNode node, String field) {
        JsonNode value = node.get(field);
        if (value == null || !value.isIntegralNumber() || !value.canConvertToInt() || value.asInt() < 0) {
            throw new IllegalStateException("Document-rule field must be a non-negative integer: " + field);
        }
        return value.asInt();
    }

    private static Integer optionalNonNegativeInt(JsonNode node, String field) {
        JsonNode value = node.get(field);
        if (value == null) {
            return null;
        }
        if (!value.isIntegralNumber() || !value.canConvertToInt() || value.asInt() < 0) {
            throw new IllegalStateException("Document-rule field must be a non-negative integer: " + field);
        }
        return value.asInt();
    }

    private static LocalDate requiredDate(JsonNode node, String field) {
        JsonNode value = node.get(field);
        if (value == null || !value.isString() || value.asString().isBlank()) {
            throw new IllegalStateException("Missing document-rule date: " + field);
        }
        return LocalDate.parse(value.asString());
    }

    private static LocalDate optionalDate(JsonNode node, String field) {
        JsonNode value = node.get(field);
        if (value == null) {
            return null;
        }
        if (!value.isString() || value.asString().isBlank()) {
            throw new IllegalStateException("Invalid document-rule date: " + field);
        }
        return LocalDate.parse(value.asString());
    }

}
