package io.github.dajoh2062.traveldb.documents;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.Resource;
import org.springframework.jdbc.core.JdbcTemplate;
import tools.jackson.databind.ObjectMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:document-rule-repository-tests;DB_CLOSE_ON_EXIT=FALSE")
class DocumentRuleRepositoryIntegrationTests {

    @Autowired
    private DocumentRuleRepository repository;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private ObjectMapper objectMapper;

    @Value("classpath:data/document-rules.json")
    private Resource snapshotResource;

    @Test
    void roundTripsTheReviewedSnapshotWithoutChangingRuleSemantics() {
        DocumentRuleSnapshot expected = DocumentRuleSnapshotLoader.load(objectMapper, snapshotResource);
        DocumentRuleSnapshot actual = repository.findActive().orElseThrow();

        assertEquals(expected, actual);
        assertTrue(actual.rules().size() > 50);
    }

    @Test
    void activatingAnExistingDatasetIsIdempotentAndKeepsOneActiveDataset() {
        DocumentRuleSnapshot active = repository.findActive().orElseThrow();

        repository.saveAndActivate(active);

        assertEquals(1, count("document_rule_datasets"));
        assertEquals(1, count("active_document_rule_dataset"));
        assertEquals(active, repository.findActive().orElseThrow());
    }

    private int count(String table) {
        Integer count = jdbc.queryForObject("SELECT COUNT(*) FROM " + table, Integer.class);
        return count == null ? 0 : count;
    }
}
