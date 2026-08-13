package io.github.dajoh2062.traveldb.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import io.github.dajoh2062.traveldb.model.Airport;

import java.util.List;
import java.util.Optional;

@Repository
public class AirportRepository {

    private static final String AIRPORT_COLUMNS = """
            source_id, ident, iata_code, icao_code, gps_code, local_code,
            name, municipality, region_code, country, country_code, continent,
            airport_type, scheduled_service, latitude_deg, longitude_deg,
            elevation_ft, official_url, wikipedia_url, keywords, is_schengen
            """;
    private static final String FIND_BY_IATA_SQL =
            "SELECT " + AIRPORT_COLUMNS + " FROM Airports WHERE iata_code = ?";
    private static final String FIND_ALL_SQL = "SELECT " + AIRPORT_COLUMNS + " FROM Airports";

    private static final RowMapper<Airport> AIRPORT_MAPPER = (resultSet, rowNumber) -> new Airport(
            resultSet.getLong("source_id"),
            resultSet.getString("ident"),
            resultSet.getString("iata_code"),
            resultSet.getString("icao_code"),
            resultSet.getString("gps_code"),
            resultSet.getString("local_code"),
            resultSet.getString("name"),
            resultSet.getString("municipality"),
            resultSet.getString("region_code"),
            resultSet.getString("country"),
            resultSet.getString("country_code"),
            resultSet.getString("continent"),
            resultSet.getString("airport_type"),
            resultSet.getBoolean("scheduled_service"),
            resultSet.getDouble("latitude_deg"),
            resultSet.getDouble("longitude_deg"),
            (Integer) resultSet.getObject("elevation_ft"),
            resultSet.getString("official_url"),
            resultSet.getString("wikipedia_url"),
            resultSet.getString("keywords"),
            resultSet.getBoolean("is_schengen")
    );

    private final JdbcTemplate jdbc;

    public AirportRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Optional<Airport> findByIataCode(String iataCode) {
        return jdbc.query(FIND_BY_IATA_SQL, AIRPORT_MAPPER, iataCode).stream().findFirst();
    }

    public List<Airport> findAll() {
        return List.copyOf(jdbc.query(FIND_ALL_SQL, AIRPORT_MAPPER));
    }
}
