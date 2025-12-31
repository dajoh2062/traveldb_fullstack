package projects.traveldbbackend;

import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

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
    String q = "%" + query.toUpperCase() + "%";

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
    return jdbc.queryForObject(
        "SELECT is_schengen FROM Countries WHERE country_id = ?",
        Boolean.class,
        countryId
    );
}


}
