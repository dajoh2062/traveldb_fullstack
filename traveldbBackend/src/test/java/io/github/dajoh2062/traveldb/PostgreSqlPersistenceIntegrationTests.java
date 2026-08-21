package io.github.dajoh2062.traveldb;

import io.github.dajoh2062.traveldb.model.Airport;
import io.github.dajoh2062.traveldb.model.Country;
import io.github.dajoh2062.traveldb.repository.AirportRepository;
import io.github.dajoh2062.traveldb.repository.CountryRepository;
import io.github.dajoh2062.traveldb.repository.DatabaseHealthRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import javax.sql.DataSource;
import java.sql.Connection;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest
class PostgreSqlPersistenceIntegrationTests {

    @Container
    private static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:18-alpine")
            .withDatabaseName("traveldb_integration")
            .withUsername("traveldb")
            .withPassword("traveldb");

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Autowired
    private DataSource dataSource;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private CountryRepository countryRepository;

    @Autowired
    private AirportRepository airportRepository;

    @Autowired
    private DatabaseHealthRepository databaseHealthRepository;

    @Test
    void appliesEveryFlywayMigrationToPostgreSql() throws Exception {
        try (Connection connection = dataSource.getConnection()) {
            assertEquals("PostgreSQL", connection.getMetaData().getDatabaseProductName());
            assertEquals(18, connection.getMetaData().getDatabaseMajorVersion());
        }

        assertEquals(4, requiredInteger("""
                SELECT COUNT(*)
                FROM flyway_schema_history
                WHERE success = TRUE
                """));
        assertEquals("4", jdbc.queryForObject("""
                SELECT version
                FROM flyway_schema_history
                WHERE success = TRUE
                ORDER BY installed_rank DESC
                LIMIT 1
                """, String.class));
    }

    @Test
    void bootstrapsAndReadsEveryPersistedDataset() {
        assertEquals(249, requiredInteger("SELECT COUNT(*) FROM countries"));
        assertEquals(9055, requiredInteger("SELECT COUNT(*) FROM airports"));

        assertEquals("2026-08-09.5", jdbc.queryForObject("""
                SELECT dataset_version
                FROM document_rule_datasets datasets
                JOIN active_document_rule_dataset active ON active.dataset_id = datasets.id
                WHERE active.slot = 1
                """, String.class));
        assertEquals(59, requiredInteger("""
                SELECT COUNT(*)
                FROM document_rules rules
                JOIN active_document_rule_dataset active ON active.dataset_id = rules.dataset_id
                WHERE active.slot = 1
                """));

        assertEquals("2026-07-31.1", jdbc.queryForObject("""
                SELECT dataset_version
                FROM active_baggage_rule_dataset
                WHERE slot = 1
                """, String.class));
        assertEquals(15, requiredInteger("""
                SELECT COUNT(*)
                FROM baggage_rules rules
                JOIN active_baggage_rule_dataset active
                    ON active.dataset_version = rules.dataset_version
                WHERE active.slot = 1
                """));

        Country norway = countryRepository.findAll().stream()
                .filter(country -> "NO".equals(country.countryId()))
                .findFirst()
                .orElseThrow();
        assertEquals("Norway", norway.countryNameEn());

        Airport oslo = airportRepository.findByIataCode("OSL").orElseThrow();
        assertEquals("ENGM", oslo.icaoCode());
        assertEquals("NO", oslo.countryCode());
        assertTrue(databaseHealthRepository.isAvailable());
    }

    private int requiredInteger(String sql) {
        Integer value = jdbc.queryForObject(sql, Integer.class);
        if (value == null) {
            throw new IllegalStateException("Expected an integer result for PostgreSQL integration query.");
        }
        return value;
    }
}
