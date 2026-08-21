package io.github.dajoh2062.traveldb.repository;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.Resource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.DatabasePopulatorUtils;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;

@Component
public class ReferenceDataBootstrap implements ApplicationRunner {

    private final JdbcTemplate jdbc;
    private final DataSource dataSource;
    private final boolean enabled;
    private final Resource countriesResource;
    private final Resource airportsResource;

    public ReferenceDataBootstrap(
            JdbcTemplate jdbc,
            DataSource dataSource,
            @Value("${traveldb.reference-data.bootstrap-enabled:true}") boolean enabled,
            @Value("classpath:data/countries.sql") Resource countriesResource,
            @Value("classpath:data/airports.sql") Resource airportsResource
    ) {
        this.jdbc = jdbc;
        this.dataSource = dataSource;
        this.enabled = enabled;
        this.countriesResource = countriesResource;
        this.airportsResource = airportsResource;
    }

    @Override
    public void run(ApplicationArguments arguments) {
        if (!enabled) {
            return;
        }
        if (countRows("countries") == 0) {
            execute(countriesResource);
        }
        if (countRows("airports") == 0) {
            execute(airportsResource);
        }
    }

    private long countRows(String table) {
        Long count = jdbc.queryForObject("SELECT COUNT(*) FROM " + table, Long.class);
        return count == null ? 0 : count;
    }

    private void execute(Resource resource) {
        ResourceDatabasePopulator populator = new ResourceDatabasePopulator();
        populator.addScript(resource);
        populator.setContinueOnError(false);
        DatabasePopulatorUtils.execute(populator, dataSource);
    }
}
