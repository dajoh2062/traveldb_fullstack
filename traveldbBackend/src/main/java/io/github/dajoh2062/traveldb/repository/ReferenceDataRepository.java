package io.github.dajoh2062.traveldb.repository;

import org.springframework.core.io.Resource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.DatabasePopulatorUtils;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;

@Repository
public class ReferenceDataRepository {

    private final JdbcTemplate jdbc;
    private final DataSource dataSource;

    public ReferenceDataRepository(JdbcTemplate jdbc, DataSource dataSource) {
        this.jdbc = jdbc;
        this.dataSource = dataSource;
    }

    public boolean countriesAreEmpty() {
        return count("SELECT COUNT(*) FROM countries") == 0;
    }

    public boolean airportsAreEmpty() {
        return count("SELECT COUNT(*) FROM airports") == 0;
    }

    public void executeImport(Resource resource) {
        ResourceDatabasePopulator populator = new ResourceDatabasePopulator();
        populator.addScript(resource);
        populator.setContinueOnError(false);
        DatabasePopulatorUtils.execute(populator, dataSource);
    }

    private long count(String sql) {
        Long count = jdbc.queryForObject(sql, Long.class);
        return count == null ? 0 : count;
    }
}
