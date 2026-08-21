package io.github.dajoh2062.traveldb.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DatabaseConfigurationTests {

    @Test
    void convertsRenderPostgresUrlsToJdbcUrlsWithoutEmbeddingCredentials() {
        assertEquals(
                "jdbc:postgresql://internal.example:5432/traveldb?sslmode=require",
                DatabaseConfiguration.toJdbcUrl(
                        "postgresql://user:secret@internal.example:5432/traveldb?sslmode=require"
                )
        );
    }

    @Test
    void leavesJdbcUrlsUnchanged() {
        String jdbcUrl = "jdbc:h2:mem:testdb";
        assertEquals(jdbcUrl, DatabaseConfiguration.toJdbcUrl(jdbcUrl));
    }

    @Test
    void rejectsIncompletePostgresUrls() {
        assertThrows(
                IllegalArgumentException.class,
                () -> DatabaseConfiguration.toJdbcUrl("postgresql://internal.example")
        );
    }
}
