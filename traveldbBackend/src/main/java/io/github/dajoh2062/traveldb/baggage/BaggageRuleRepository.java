package io.github.dajoh2062.traveldb.baggage;

import io.github.dajoh2062.traveldb.baggage.BaggageAdvice.AdviceCode;
import io.github.dajoh2062.traveldb.baggage.BaggageAdvice.Source;
import io.github.dajoh2062.traveldb.baggage.BaggageAdvice.Status;
import io.github.dajoh2062.traveldb.baggage.BaggageCheckRequest.ThroughCheckStatus;
import io.github.dajoh2062.traveldb.baggage.BaggageCheckRequest.TicketArrangement;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Repository
public class BaggageRuleRepository {

    private static final String FIND_ACTIVE_DATASET_SQL = """
            SELECT datasets.dataset_version, datasets.reviewed_date
            FROM active_baggage_rule_dataset active
            JOIN baggage_rule_datasets datasets
              ON datasets.dataset_version = active.dataset_version
            WHERE active.slot = 1
            """;
    private static final String FIND_RULES_SQL = """
            SELECT rule_id, rule_position, priority, entering_country, onward_domestic,
                   current_country_code, current_airport_code, previous_airport_code,
                   previous_airport_group, ticket_arrangement, through_check_status,
                   advice_code, advice_status, title, explanation
            FROM baggage_rules
            WHERE dataset_version = ?
            ORDER BY priority DESC, rule_position
            """;

    private final JdbcTemplate jdbc;

    public BaggageRuleRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Optional<BaggageRuleSnapshot> findActive() {
        return jdbc.query(FIND_ACTIVE_DATASET_SQL, (resultSet, rowNumber) -> new DatasetRow(
                        resultSet.getString("dataset_version"),
                        resultSet.getDate("reviewed_date").toLocalDate()
                )).stream()
                .findFirst()
                .map(this::loadSnapshot);
    }

    Optional<String> findActiveDatasetVersion() {
        return jdbc.queryForList("""
                SELECT dataset_version
                FROM active_baggage_rule_dataset
                WHERE slot = 1
                """, String.class).stream().findFirst();
    }

    private BaggageRuleSnapshot loadSnapshot(DatasetRow dataset) {
        Map<String, Set<String>> groups = loadAirportGroups(dataset.version());
        List<RuleRow> rows = jdbc.query(FIND_RULES_SQL, (resultSet, rowNumber) -> new RuleRow(
                resultSet.getString("rule_id"),
                resultSet.getInt("rule_position"),
                resultSet.getInt("priority"),
                (Boolean) resultSet.getObject("entering_country"),
                (Boolean) resultSet.getObject("onward_domestic"),
                resultSet.getString("current_country_code"),
                resultSet.getString("current_airport_code"),
                resultSet.getString("previous_airport_code"),
                resultSet.getString("previous_airport_group"),
                enumValue(TicketArrangement.class, resultSet.getString("ticket_arrangement")),
                enumValue(ThroughCheckStatus.class, resultSet.getString("through_check_status")),
                AdviceCode.valueOf(resultSet.getString("advice_code")),
                Status.valueOf(resultSet.getString("advice_status")),
                resultSet.getString("title"),
                resultSet.getString("explanation")
        ), dataset.version());
        List<BaggageRule> rules = rows.stream()
                .map(row -> loadRule(dataset.version(), row))
                .toList();
        return new BaggageRuleSnapshot(
                dataset.version(),
                dataset.reviewedDate(),
                groups,
                List.copyOf(rules)
        );
    }

    private Map<String, Set<String>> loadAirportGroups(String datasetVersion) {
        Map<String, Set<String>> groups = new LinkedHashMap<>();
        jdbc.query("""
                SELECT group_code, airport_code
                FROM baggage_airport_group_members
                WHERE dataset_version = ?
                ORDER BY group_code, airport_code
                """, resultSet -> {
            groups.computeIfAbsent(resultSet.getString("group_code"), ignored -> new LinkedHashSet<>())
                    .add(resultSet.getString("airport_code"));
        }, datasetVersion);

        Map<String, Set<String>> immutable = new LinkedHashMap<>();
        groups.forEach((key, value) -> immutable.put(key, Set.copyOf(value)));
        return Map.copyOf(immutable);
    }

    private BaggageRule loadRule(String datasetVersion, RuleRow row) {
        List<String> exceptions = jdbc.queryForList("""
                SELECT exception_text
                FROM baggage_rule_exceptions
                WHERE dataset_version = ? AND rule_id = ?
                ORDER BY exception_position
                """, String.class, datasetVersion, row.id());
        List<Source> sources = jdbc.query("""
                SELECT sources.label, sources.url
                FROM baggage_rule_sources rule_sources
                JOIN baggage_sources sources
                  ON sources.dataset_version = rule_sources.dataset_version
                 AND sources.source_key = rule_sources.source_key
                WHERE rule_sources.dataset_version = ? AND rule_sources.rule_id = ?
                ORDER BY rule_sources.source_position
                """, (resultSet, rowNumber) -> new Source(
                resultSet.getString("label"),
                resultSet.getString("url")
        ), datasetVersion, row.id());

        return new BaggageRule(
                row.id(),
                row.position(),
                row.priority(),
                row.enteringCountry(),
                row.onwardDomestic(),
                row.currentCountryCode(),
                row.currentAirportCode(),
                row.previousAirportCode(),
                row.previousAirportGroup(),
                row.ticketArrangement(),
                row.throughCheckStatus(),
                row.adviceCode(),
                row.status(),
                row.title(),
                row.explanation(),
                List.copyOf(exceptions),
                List.copyOf(sources)
        );
    }

    private static <T extends Enum<T>> T enumValue(Class<T> type, String value) {
        return value == null ? null : Enum.valueOf(type, value);
    }

    private record DatasetRow(String version, LocalDate reviewedDate) {}

    private record RuleRow(
            String id,
            int position,
            int priority,
            Boolean enteringCountry,
            Boolean onwardDomestic,
            String currentCountryCode,
            String currentAirportCode,
            String previousAirportCode,
            String previousAirportGroup,
            TicketArrangement ticketArrangement,
            ThroughCheckStatus throughCheckStatus,
            AdviceCode adviceCode,
            Status status,
            String title,
            String explanation
    ) {}
}
