package projects.traveldbbackend;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class TravelRepository {

    private final JdbcTemplate jdbc;

    public TravelRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    private static final RowMapper<Airport> AIRPORT_MAPPER = (rs, rowNum) -> new Airport(
            rs.getString("iata_code"),
            rs.getString("name"),
            rs.getString("country"),
            rs.getString("country_code"),
            rs.getBoolean("is_schengen")
    );

    public Airport getAirport(String iata) {
        return jdbc.queryForObject(
                "SELECT * FROM Airports WHERE iata_code = ?",
                AIRPORT_MAPPER,
                iata
        );
    }

    public List<Airport> searchAirports(String query) {
        if (query == null || query.trim().length() < 2) {
            return List.of();
        }

        String q = "%" + query.trim().toUpperCase() + "%";

        return jdbc.query(
                """
                SELECT * FROM Airports
                WHERE UPPER(iata_code) LIKE ?
                   OR UPPER(name) LIKE ?
                   OR UPPER(country) LIKE ?
                ORDER BY iata_code
                LIMIT 10
                """,
                AIRPORT_MAPPER,
                q, q, q
        );
    }

    public Boolean isSchengenCountry(String countryId) {
        if (countryId == null || countryId.isBlank()) return false;

        // Countries.country_id in your schema contains ISO-like codes
        // (e.g. "NO", "US", "GB").
        return jdbc.queryForObject(
                "SELECT is_schengen FROM Countries WHERE country_id = ?",
                Boolean.class,
                countryId.trim().toUpperCase()
        );
    }
}
