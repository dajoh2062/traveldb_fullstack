package projects.traveldbbackend;

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
}
