package io.github.dajoh2062.traveldb.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class DatabaseHealthRepository {

    private final JdbcTemplate jdbc;

    public DatabaseHealthRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public boolean isAvailable() {
        Integer result = jdbc.queryForObject("SELECT 1", Integer.class);
        return result != null && result == 1;
    }
}
