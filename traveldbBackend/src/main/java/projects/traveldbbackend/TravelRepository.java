package projects.traveldbbackend;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.text.Normalizer;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

@Repository
public class TravelRepository {

    private final JdbcTemplate jdbc;

    public TravelRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    private static final RowMapper<Country> COUNTRY_MAPPER = (rs, rowNum) -> new Country(
            rs.getLong("source_id"),
            rs.getString("country_id"),
            rs.getString("country_name_en"),
            rs.getString("continent"),
            rs.getString("wikipedia_url"),
            rs.getString("keywords"),
            rs.getBoolean("is_schengen")
    );

    private static final RowMapper<Airport> AIRPORT_MAPPER = (rs, rowNum) -> {
        return new Airport(
                rs.getLong("source_id"),
                rs.getString("ident"),
                rs.getString("iata_code"),
                rs.getString("icao_code"),
                rs.getString("gps_code"),
                rs.getString("local_code"),
                rs.getString("name"),
                rs.getString("municipality"),
                rs.getString("region_code"),
                rs.getString("country"),
                rs.getString("country_code"),
                rs.getString("continent"),
                rs.getString("airport_type"),
                rs.getBoolean("scheduled_service"),
                rs.getDouble("latitude_deg"),
                rs.getDouble("longitude_deg"),
                (Integer) rs.getObject("elevation_ft"),
                rs.getString("official_url"),
                rs.getString("wikipedia_url"),
                rs.getString("keywords"),
                rs.getBoolean("is_schengen")
        );
    };

    private volatile List<AirportSearchEntry> airportSearchIndex;

    public Airport getAirport(String iata) {
        return jdbc.queryForObject(
                "SELECT * FROM Airports WHERE iata_code = ?",
                AIRPORT_MAPPER,
                iata
        );
    }

    public List<Airport> searchAirports(String query) {
        String normalizedQuery = normalize(query);
        if (normalizedQuery.isBlank()) {
            return List.of();
        }

        return getAirportSearchIndex().stream()
                .map(entry -> new AirportMatch(entry.airport(), matchScore(entry, normalizedQuery)))
                .filter(match -> match.score() < Integer.MAX_VALUE)
                .sorted(Comparator.comparingInt(AirportMatch::score)
                        .thenComparing(match -> !match.airport().isScheduledService())
                        .thenComparingInt(match -> airportTypePriority(match.airport().getAirportType()))
                        .thenComparing(match -> match.airport().getIataCode()))
                .map(AirportMatch::airport)
                .toList();
    }

    public List<Country> getCountries() {
        return jdbc.query(
                "SELECT source_id, country_id, country_name_en, continent, wikipedia_url, keywords, is_schengen FROM Countries ORDER BY country_name_en",
                COUNTRY_MAPPER
        );
    }

    private List<AirportSearchEntry> getAirportSearchIndex() {
        List<AirportSearchEntry> cached = airportSearchIndex;
        if (cached == null) {
            synchronized (this) {
                cached = airportSearchIndex;
                if (cached == null) {
                    cached = jdbc.query("SELECT * FROM Airports", AIRPORT_MAPPER).stream()
                            .map(AirportSearchEntry::from)
                            .toList();
                    airportSearchIndex = cached;
                }
            }
        }
        return cached;
    }

    private static int matchScore(AirportSearchEntry airport, String query) {
        if (airport.iataCode().equals(query)) return 0;
        if (airport.alternateCodes().contains(query)) return 1;
        if (airport.iataCode().startsWith(query)) return 2;
        if (airport.city().equals(query)) return 3;
        if (airport.name().equals(query)) return 4;
        if (airport.city().startsWith(query)) return 5;
        if (airport.name().startsWith(query)) return 6;
        if (airport.countryCode().equals(query)) return 7;
        if (airport.country().startsWith(query)) return 8;
        if (airport.regionCode().equals(query)) return 9;
        if (airport.city().contains(query)) return 10;
        if (airport.name().contains(query)) return 11;
        if (airport.keywords().contains(query)) return 12;
        if (airport.country().contains(query)) return 13;
        return Integer.MAX_VALUE;
    }

    private static int airportTypePriority(String airportType) {
        if ("large_airport".equals(airportType)) return 0;
        if ("medium_airport".equals(airportType)) return 1;
        if ("small_airport".equals(airportType)) return 2;
        return 3;
    }

    private static String normalize(String value) {
        if (value == null) return "";
        return Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .trim()
                .toLowerCase(Locale.ROOT);
    }

    private record AirportMatch(Airport airport, int score) {}

    private record AirportSearchEntry(
            Airport airport,
            String iataCode,
            List<String> alternateCodes,
            String name,
            String city,
            String regionCode,
            String country,
            String countryCode,
            String keywords
    ) {
        private static AirportSearchEntry from(Airport airport) {
            return new AirportSearchEntry(
                    airport,
                    normalize(airport.getIataCode()),
                    List.of(
                            normalize(airport.getIdent()),
                            normalize(airport.getIcaoCode()),
                            normalize(airport.getGpsCode()),
                            normalize(airport.getLocalCode())
                    ),
                    normalize(airport.getName()),
                    normalize(airport.getCity()),
                    normalize(airport.getRegionCode()),
                    normalize(airport.getCountry()),
                    normalize(airport.getCountryCode()),
                    normalize(airport.getKeywords())
            );
        }
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
