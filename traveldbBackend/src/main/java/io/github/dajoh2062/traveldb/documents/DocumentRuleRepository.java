package io.github.dajoh2062.traveldb.documents;

import io.github.dajoh2062.traveldb.documents.DocumentRequirement.Category;
import io.github.dajoh2062.traveldb.documents.DocumentRequirement.DocumentSource;
import io.github.dajoh2062.traveldb.documents.DocumentRequirement.KeyFact;
import io.github.dajoh2062.traveldb.documents.DocumentRequirement.Scope;
import io.github.dajoh2062.traveldb.documents.DocumentRequirement.Status;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public class DocumentRuleRepository {

    private static final String INSERT_DATASET_SQL = """
            INSERT INTO document_rule_datasets (dataset_version, generated_at)
            VALUES (?, ?)
            """;
    private static final String FIND_ACTIVE_DATASET_SQL = """
            SELECT d.id, d.dataset_version, d.generated_at
            FROM active_document_rule_dataset active
            JOIN document_rule_datasets d ON d.id = active.dataset_id
            WHERE active.slot = 1
            """;
    private static final String FIND_RULES_SQL = """
            SELECT rule_id, decision_key, scope, priority, effective_from, effective_to,
                   last_verified, review_after, minimum_age, maximum_age, output_code,
                   category, output_status, title, summary
            FROM document_rules
            WHERE dataset_id = ?
            ORDER BY rule_position
            """;

    private final JdbcTemplate jdbc;

    public DocumentRuleRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Optional<DocumentRuleSnapshot> findActive() {
        return jdbc.query(FIND_ACTIVE_DATASET_SQL, (resultSet, rowNumber) -> new DatasetRow(
                        resultSet.getLong("id"),
                        resultSet.getString("dataset_version"),
                        resultSet.getTimestamp("generated_at").toInstant()
                )).stream()
                .findFirst()
                .map(this::loadSnapshot);
    }

    Optional<String> findActiveDatasetVersion() {
        return jdbc.queryForList("""
                SELECT datasets.dataset_version
                FROM active_document_rule_dataset active
                JOIN document_rule_datasets datasets ON datasets.id = active.dataset_id
                WHERE active.slot = 1
                """, String.class).stream().findFirst();
    }

    private boolean datasetExists(String datasetVersion) {
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM document_rule_datasets WHERE dataset_version = ?",
                Integer.class,
                datasetVersion
        );
        return count != null && count > 0;
    }

    void saveAndActivate(DocumentRuleSnapshot snapshot) {
        if (datasetExists(snapshot.datasetVersion())) {
            activateExisting(snapshot.datasetVersion());
            return;
        }

        long datasetId = insertDataset(snapshot);
        insertDatasetSources(datasetId, snapshot.sources());
        for (int index = 0; index < snapshot.rules().size(); index++) {
            DocumentRule rule = snapshot.rules().get(index);
            insertRule(datasetId, index, rule);
            insertSelectors(datasetId, rule);
            insertConditions(datasetId, rule.id(), rule.conditions());
            insertSources(datasetId, rule.id(), rule.sources());
            insertKeyFacts(datasetId, rule.id(), rule.keyFacts());
        }
        activate(datasetId);
    }

    private DocumentRuleSnapshot loadSnapshot(DatasetRow dataset) {
        List<DocumentSource> datasetSources = jdbc.query("""
                SELECT label, url, source_type
                FROM document_rule_dataset_sources
                WHERE dataset_id = ?
                ORDER BY source_position
                """, (resultSet, rowNumber) -> source(
                resultSet.getString("label"),
                resultSet.getString("url"),
                resultSet.getString("source_type")
        ), dataset.id());

        List<RuleRow> rows = jdbc.query(FIND_RULES_SQL, (resultSet, rowNumber) -> new RuleRow(
                resultSet.getString("rule_id"),
                resultSet.getString("decision_key"),
                Scope.valueOf(resultSet.getString("scope")),
                resultSet.getInt("priority"),
                localDate(resultSet.getDate("effective_from")),
                localDate(resultSet.getDate("effective_to")),
                localDate(resultSet.getDate("last_verified")),
                localDate(resultSet.getDate("review_after")),
                (Integer) resultSet.getObject("minimum_age"),
                (Integer) resultSet.getObject("maximum_age"),
                resultSet.getString("output_code"),
                Category.valueOf(resultSet.getString("category")),
                Status.valueOf(resultSet.getString("output_status")),
                resultSet.getString("title"),
                resultSet.getString("summary")
        ), dataset.id());

        List<DocumentRule> rules = rows.stream()
                .map(row -> loadRule(dataset.id(), row))
                .toList();
        return new DocumentRuleSnapshot(
                dataset.version(),
                dataset.generatedAt(),
                List.copyOf(datasetSources),
                List.copyOf(rules)
        );
    }

    private DocumentRule loadRule(long datasetId, RuleRow row) {
        return new DocumentRule(
                row.id(),
                row.decisionKey(),
                row.scope(),
                selectors(datasetId, row.id(), SelectorType.DESTINATION_COUNTRY),
                selectors(datasetId, row.id(), SelectorType.NATIONALITY),
                selectors(datasetId, row.id(), SelectorType.EXCLUDED_NATIONALITY),
                selectors(datasetId, row.id(), SelectorType.RESIDENCE_COUNTRY),
                selectors(datasetId, row.id(), SelectorType.PASSPORT_ISSUING_COUNTRY),
                selectors(datasetId, row.id(), SelectorType.TRAVEL_PURPOSE),
                row.minimumAge(),
                row.maximumAge(),
                selectors(datasetId, row.id(), SelectorType.REQUIRED_HELD_VISA_COUNTRY),
                selectors(datasetId, row.id(), SelectorType.REQUIRED_RESIDENCE_PERMIT_COUNTRY),
                row.priority(),
                row.effectiveFrom(),
                row.effectiveTo(),
                row.lastVerified(),
                row.reviewAfter(),
                row.outputCode(),
                row.category(),
                row.status(),
                row.title(),
                row.summary(),
                strings(datasetId, row.id(), "document_rule_conditions", "condition_text", "condition_position"),
                ruleSources(datasetId, row.id()),
                keyFacts(datasetId, row.id())
        );
    }

    private List<String> selectors(long datasetId, String ruleId, SelectorType type) {
        return jdbc.queryForList("""
                SELECT selector_value
                FROM document_rule_selectors
                WHERE dataset_id = ? AND rule_id = ? AND selector_type = ?
                ORDER BY selector_position
                """, String.class, datasetId, ruleId, type.name());
    }

    private List<String> strings(long datasetId, String ruleId, String table, String valueColumn, String orderColumn) {
        return jdbc.queryForList(
                "SELECT " + valueColumn + " FROM " + table
                        + " WHERE dataset_id = ? AND rule_id = ? ORDER BY " + orderColumn,
                String.class,
                datasetId,
                ruleId
        );
    }

    private List<DocumentSource> ruleSources(long datasetId, String ruleId) {
        return jdbc.query("""
                SELECT label, url, source_type
                FROM document_rule_sources
                WHERE dataset_id = ? AND rule_id = ?
                ORDER BY source_position
                """, (resultSet, rowNumber) -> source(
                resultSet.getString("label"),
                resultSet.getString("url"),
                resultSet.getString("source_type")
        ), datasetId, ruleId);
    }

    private List<KeyFact> keyFacts(long datasetId, String ruleId) {
        return jdbc.query("""
                SELECT label, fact_value
                FROM document_rule_key_facts
                WHERE dataset_id = ? AND rule_id = ?
                ORDER BY fact_position
                """, (resultSet, rowNumber) -> new KeyFact(
                resultSet.getString("label"),
                resultSet.getString("fact_value")
        ), datasetId, ruleId);
    }

    private long insertDataset(DocumentRuleSnapshot snapshot) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbc.update(connection -> {
            PreparedStatement statement = connection.prepareStatement(INSERT_DATASET_SQL, new String[]{"id"});
            statement.setString(1, snapshot.datasetVersion());
            statement.setTimestamp(2, Timestamp.from(snapshot.generatedAt()));
            return statement;
        }, keyHolder);
        Number key = keyHolder.getKey();
        if (key == null) {
            throw new IllegalStateException("Database did not return the inserted document-rule dataset ID.");
        }
        return key.longValue();
    }

    private void insertDatasetSources(long datasetId, List<DocumentSource> sources) {
        for (int index = 0; index < sources.size(); index++) {
            DocumentSource source = sources.get(index);
            jdbc.update("""
                    INSERT INTO document_rule_dataset_sources
                        (dataset_id, source_position, label, url, source_type)
                    VALUES (?, ?, ?, ?, ?)
                    """, datasetId, index, source.label(), source.url(), source.sourceType());
        }
    }

    private void insertRule(long datasetId, int rulePosition, DocumentRule rule) {
        jdbc.update("""
                INSERT INTO document_rules (
                    dataset_id, rule_id, rule_position, decision_key, scope, priority, effective_from, effective_to,
                    last_verified, review_after, minimum_age, maximum_age, output_code, category,
                    output_status, title, summary
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                datasetId,
                rule.id(),
                rulePosition,
                rule.decisionKey(),
                rule.scope().name(),
                rule.priority(),
                sqlDate(rule.effectiveFrom()),
                sqlDate(rule.effectiveTo()),
                sqlDate(rule.lastVerified()),
                sqlDate(rule.reviewAfter()),
                rule.minimumAge(),
                rule.maximumAge(),
                rule.code(),
                rule.category().name(),
                rule.status().name(),
                rule.title(),
                rule.summary()
        );
    }

    private void insertSelectors(long datasetId, DocumentRule rule) {
        insertSelectorValues(datasetId, rule.id(), SelectorType.DESTINATION_COUNTRY, rule.destinationCountries());
        insertSelectorValues(datasetId, rule.id(), SelectorType.NATIONALITY, rule.nationalities());
        insertSelectorValues(datasetId, rule.id(), SelectorType.EXCLUDED_NATIONALITY, rule.excludedNationalities());
        insertSelectorValues(datasetId, rule.id(), SelectorType.RESIDENCE_COUNTRY, rule.residenceCountries());
        insertSelectorValues(datasetId, rule.id(), SelectorType.PASSPORT_ISSUING_COUNTRY, rule.passportIssuingCountries());
        insertSelectorValues(datasetId, rule.id(), SelectorType.TRAVEL_PURPOSE, rule.travelPurposes());
        insertSelectorValues(datasetId, rule.id(), SelectorType.REQUIRED_HELD_VISA_COUNTRY, rule.requiredHeldVisaCountries());
        insertSelectorValues(datasetId, rule.id(), SelectorType.REQUIRED_RESIDENCE_PERMIT_COUNTRY, rule.requiredResidencePermitCountries());
    }

    private void insertSelectorValues(long datasetId, String ruleId, SelectorType type, List<String> values) {
        for (int index = 0; index < values.size(); index++) {
            jdbc.update("""
                    INSERT INTO document_rule_selectors
                        (dataset_id, rule_id, selector_type, selector_position, selector_value)
                    VALUES (?, ?, ?, ?, ?)
                    """, datasetId, ruleId, type.name(), index, values.get(index));
        }
    }

    private void insertConditions(long datasetId, String ruleId, List<String> conditions) {
        for (int index = 0; index < conditions.size(); index++) {
            jdbc.update("""
                    INSERT INTO document_rule_conditions
                        (dataset_id, rule_id, condition_position, condition_text)
                    VALUES (?, ?, ?, ?)
                    """, datasetId, ruleId, index, conditions.get(index));
        }
    }

    private void insertSources(long datasetId, String ruleId, List<DocumentSource> sources) {
        for (int index = 0; index < sources.size(); index++) {
            DocumentSource source = sources.get(index);
            jdbc.update("""
                    INSERT INTO document_rule_sources
                        (dataset_id, rule_id, source_position, label, url, source_type)
                    VALUES (?, ?, ?, ?, ?, ?)
                    """, datasetId, ruleId, index, source.label(), source.url(), source.sourceType());
        }
    }

    private void insertKeyFacts(long datasetId, String ruleId, List<KeyFact> keyFacts) {
        for (int index = 0; index < keyFacts.size(); index++) {
            KeyFact fact = keyFacts.get(index);
            jdbc.update("""
                    INSERT INTO document_rule_key_facts
                        (dataset_id, rule_id, fact_position, label, fact_value)
                    VALUES (?, ?, ?, ?, ?)
                    """, datasetId, ruleId, index, fact.label(), fact.value());
        }
    }

    private void activateExisting(String datasetVersion) {
        Long datasetId = jdbc.queryForObject(
                "SELECT id FROM document_rule_datasets WHERE dataset_version = ?",
                Long.class,
                datasetVersion
        );
        if (datasetId == null) {
            throw new IllegalStateException("Document-rule dataset disappeared during activation: " + datasetVersion);
        }
        activate(datasetId);
    }

    private void activate(long datasetId) {
        jdbc.update("DELETE FROM active_document_rule_dataset WHERE slot = 1");
        jdbc.update(
                "INSERT INTO active_document_rule_dataset (slot, dataset_id) VALUES (1, ?)",
                datasetId
        );
    }

    private static DocumentSource source(String label, String url, String sourceType) {
        return new DocumentSource(label, url, sourceType);
    }

    private static Date sqlDate(LocalDate value) {
        return value == null ? null : Date.valueOf(value);
    }

    private static LocalDate localDate(Date value) {
        return value == null ? null : value.toLocalDate();
    }

    private enum SelectorType {
        DESTINATION_COUNTRY,
        NATIONALITY,
        EXCLUDED_NATIONALITY,
        RESIDENCE_COUNTRY,
        PASSPORT_ISSUING_COUNTRY,
        TRAVEL_PURPOSE,
        REQUIRED_HELD_VISA_COUNTRY,
        REQUIRED_RESIDENCE_PERMIT_COUNTRY
    }

    private record DatasetRow(long id, String version, java.time.Instant generatedAt) {}

    private record RuleRow(
            String id,
            String decisionKey,
            Scope scope,
            int priority,
            LocalDate effectiveFrom,
            LocalDate effectiveTo,
            LocalDate lastVerified,
            LocalDate reviewAfter,
            Integer minimumAge,
            Integer maximumAge,
            String outputCode,
            Category category,
            Status status,
            String title,
            String summary
    ) {}
}
