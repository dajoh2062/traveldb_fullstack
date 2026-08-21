package io.github.dajoh2062.traveldb.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import io.github.dajoh2062.traveldb.model.Country;

import java.util.List;
import java.util.Locale;

@Repository
public class CountryRepository {

    private static final String FIND_ALL_SQL = """
            SELECT source_id, country_id, country_name_en, continent, wikipedia_url, keywords, is_schengen
            FROM Countries
            ORDER BY country_name_en
            """;

    private static final RowMapper<Country> COUNTRY_MAPPER = (resultSet, rowNumber) -> new Country(
            resultSet.getLong("source_id"),
            resultSet.getString("country_id"),
            resultSet.getString("country_name_en"),
            resultSet.getString("continent"),
            resultSet.getString("wikipedia_url"),
            resultSet.getString("keywords"),
            resultSet.getBoolean("is_schengen")
    );

    private final JdbcTemplate jdbc;

    public CountryRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<Country> findAll() {
        return List.copyOf(jdbc.query(FIND_ALL_SQL, COUNTRY_MAPPER));
    }

    public boolean existsByCountryCode(String countryCode) {
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM countries WHERE country_id = ?",
                Integer.class,
                countryCode.trim().toUpperCase(Locale.ROOT)
        );
        return count != null && count > 0;
    }
}
