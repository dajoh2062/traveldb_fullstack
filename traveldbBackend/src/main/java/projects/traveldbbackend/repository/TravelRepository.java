package projects.traveldbbackend.repository;

import jakarta.annotation.PostConstruct;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import projects.traveldbbackend.model.Airport;
import projects.traveldbbackend.model.Country;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

@Repository
public class TravelRepository {

    private static final String FIND_AIRPORT_SQL = "SELECT * FROM Airports WHERE iata_code = ?";
    private static final String LOAD_AIRPORTS_SQL = "SELECT * FROM Airports";
    private static final String LOAD_COUNTRIES_SQL = """
            SELECT source_id, country_id, country_name_en, continent, wikipedia_url, keywords, is_schengen
            FROM Countries
            ORDER BY country_name_en
            """;
    private static final int SEARCH_CACHE_CAPACITY = 128;
    private static final int NO_MATCH = Integer.MAX_VALUE;
    private static final Pattern DIACRITICS = Pattern.compile("\\p{M}+");
    private static final Comparator<AirportMatch> AIRPORT_MATCH_ORDER = Comparator
            .comparingInt(AirportMatch::score)
            .thenComparing(match -> !match.airport().isScheduledService())
            .thenComparingInt(match -> airportTypePriority(match.airport().getAirportType()))
            .thenComparing(match -> match.airport().getIataCode());

    private static final RowMapper<Country> COUNTRY_MAPPER = (resultSet, rowNumber) -> new Country(
            resultSet.getLong("source_id"),
            resultSet.getString("country_id"),
            resultSet.getString("country_name_en"),
            resultSet.getString("continent"),
            resultSet.getString("wikipedia_url"),
            resultSet.getString("keywords"),
            resultSet.getBoolean("is_schengen")
    );

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
    private final Map<String, List<Airport>> airportSearchCache = Collections.synchronizedMap(
            new LinkedHashMap<>(SEARCH_CACHE_CAPACITY, 0.75f, true) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<String, List<Airport>> eldest) {
                    return size() > SEARCH_CACHE_CAPACITY;
                }
            }
    );
    private volatile List<AirportSearchEntry> airportSearchIndex;
    private volatile List<Country> countries;

    public TravelRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @PostConstruct
    void warmSearchData() {
        airportSearchIndex();
        countries();
    }

    public Airport getAirport(String iataCode) {
        return jdbc.queryForObject(FIND_AIRPORT_SQL, AIRPORT_MAPPER, iataCode);
    }

    public Optional<Airport> findAirport(String iataCode) {
        return jdbc.query(FIND_AIRPORT_SQL, AIRPORT_MAPPER, iataCode).stream().findFirst();
    }

    public List<Airport> searchAirports(String query) {
        String normalizedQuery = normalize(query);
        if (normalizedQuery.isBlank()) {
            return List.of();
        }

        List<Airport> cachedResult = cachedSearch(normalizedQuery);
        if (cachedResult != null) {
            return cachedResult;
        }

        List<AirportMatch> matches = new ArrayList<>();
        for (AirportSearchEntry entry : airportSearchIndex()) {
            int score = matchScore(entry, normalizedQuery);
            if (score != NO_MATCH) {
                matches.add(new AirportMatch(entry.airport(), score));
            }
        }
        matches.sort(AIRPORT_MATCH_ORDER);

        List<Airport> result = matches.stream()
                .map(AirportMatch::airport)
                .toList();
        return cacheSearch(normalizedQuery, result);
    }

    public List<Country> getCountries() {
        return countries();
    }

    public boolean countryExists(String countryCode) {
        if (countryCode == null || countryCode.isBlank()) {
            return false;
        }

        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM Countries WHERE country_id = ?",
                Integer.class,
                countryCode.trim().toUpperCase(Locale.ROOT)
        );
        return count != null && count > 0;
    }

    private List<AirportSearchEntry> airportSearchIndex() {
        List<AirportSearchEntry> cachedIndex = airportSearchIndex;
        if (cachedIndex == null) {
            synchronized (this) {
                cachedIndex = airportSearchIndex;
                if (cachedIndex == null) {
                    cachedIndex = jdbc.query(LOAD_AIRPORTS_SQL, AIRPORT_MAPPER).stream()
                            .map(AirportSearchEntry::from)
                            .toList();
                    airportSearchIndex = cachedIndex;
                }
            }
        }
        return cachedIndex;
    }

    private List<Country> countries() {
        List<Country> cachedCountries = countries;
        if (cachedCountries == null) {
            synchronized (this) {
                cachedCountries = countries;
                if (cachedCountries == null) {
                    cachedCountries = List.copyOf(jdbc.query(LOAD_COUNTRIES_SQL, COUNTRY_MAPPER));
                    countries = cachedCountries;
                }
            }
        }
        return cachedCountries;
    }

    private List<Airport> cachedSearch(String normalizedQuery) {
        synchronized (airportSearchCache) {
            return airportSearchCache.get(normalizedQuery);
        }
    }

    private List<Airport> cacheSearch(String normalizedQuery, List<Airport> result) {
        synchronized (airportSearchCache) {
            List<Airport> existingResult = airportSearchCache.get(normalizedQuery);
            if (existingResult != null) {
                return existingResult;
            }
            airportSearchCache.put(normalizedQuery, result);
            return result;
        }
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
        return NO_MATCH;
    }

    private static int airportTypePriority(String airportType) {
        if (airportType == null) {
            return 3;
        }
        return switch (airportType) {
            case "large_airport" -> 0;
            case "medium_airport" -> 1;
            case "small_airport" -> 2;
            default -> 3;
        };
    }

    private static String normalize(String value) {
        if (value == null) {
            return "";
        }

        String decomposed = Normalizer.normalize(value, Normalizer.Form.NFD);
        return DIACRITICS.matcher(decomposed)
                .replaceAll("")
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
}
