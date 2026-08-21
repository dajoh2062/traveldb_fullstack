package io.github.dajoh2062.traveldb.baggage;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:baggage-rule-repository-tests;DB_CLOSE_ON_EXIT=FALSE")
class BaggageRuleRepositoryIntegrationTests {

    @Autowired
    private BaggageRuleRepository repository;

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void loadsTheCompleteActiveDatasetFromRelationalTables() {
        BaggageRuleSnapshot snapshot = repository.findActive().orElseThrow();

        assertEquals("2026-07-31.1", snapshot.datasetVersion());
        assertEquals(LocalDate.of(2026, 7, 31), snapshot.reviewedDate());
        assertEquals(15, snapshot.rules().size());
        assertEquals(15, snapshot.airportGroups().get("US_PRECLEARANCE").size());
        assertTrue(snapshot.airportGroups().get("US_PRECLEARANCE").contains("DUB"));
        assertTrue(snapshot.rules().stream().allMatch(rule -> !rule.sources().isEmpty()));
    }

    @Test
    void keepsExactlyOneActiveDatasetAndOneFallbackRule() {
        assertEquals(1, count("active_baggage_rule_dataset"));
        assertEquals(1, jdbc.queryForObject("""
                SELECT COUNT(*)
                FROM baggage_rules
                WHERE dataset_version = '2026-07-31.1' AND priority = 0
                """, Integer.class));
    }

    private int count(String table) {
        Integer count = jdbc.queryForObject("SELECT COUNT(*) FROM " + table, Integer.class);
        return count == null ? 0 : count;
    }
}
