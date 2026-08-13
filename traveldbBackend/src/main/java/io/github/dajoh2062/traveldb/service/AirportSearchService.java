package io.github.dajoh2062.traveldb.service;

import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;
import io.github.dajoh2062.traveldb.api.InvalidRequestParameterException;
import io.github.dajoh2062.traveldb.api.dto.AirportSearchItem;
import io.github.dajoh2062.traveldb.api.dto.AirportSearchResponse;
import io.github.dajoh2062.traveldb.model.Airport;
import io.github.dajoh2062.traveldb.repository.AirportRepository;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

@Service
public class AirportSearchService {

    public static final int MAX_QUERY_LENGTH = 100;

    private static final int MAX_RESULTS_PER_PAGE = 100;
    private static final int SEARCH_CACHE_CAPACITY = 128;
    private static final int NO_MATCH = Integer.MAX_VALUE;
    private static final Pattern DIACRITICS = Pattern.compile("\\p{M}+");
    private static final Comparator<AirportMatch> MATCH_ORDER = Comparator
            .comparingInt(AirportMatch::score)
            .thenComparing(match -> !match.airport().scheduledService())
            .thenComparingInt(match -> airportTypePriority(match.airport().airportType()))
            .thenComparing(match -> match.airport().iataCode());

    private final AirportRepository repository;
    private final Map<String, List<Airport>> searchCache = Collections.synchronizedMap(
            new LinkedHashMap<>(SEARCH_CACHE_CAPACITY, 0.75f, true) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<String, List<Airport>> eldest) {
                    return size() > SEARCH_CACHE_CAPACITY;
                }
            }
    );
    private List<AirportSearchEntry> searchIndex = List.of();

    public AirportSearchService(AirportRepository repository) {
        this.repository = repository;
    }

    @PostConstruct
    void buildSearchIndex() {
        searchIndex = repository.findAll().stream()
                .map(AirportSearchEntry::from)
                .toList();
    }

    public AirportSearchResponse searchAirports(String query, int offset, int limit) {
        int pageOffset = Math.max(0, offset);
        int pageSize = Math.max(1, Math.min(limit, MAX_RESULTS_PER_PAGE));
        String searchQuery = query == null ? "" : query.trim();
        if (searchQuery.length() > MAX_QUERY_LENGTH) {
            throw new InvalidRequestParameterException(
                    "q",
                    "Search text cannot exceed " + MAX_QUERY_LENGTH + " characters."
            );
        }

        List<Airport> matches = searchQuery.isEmpty() ? List.of() : rankedMatches(searchQuery);
        int fromIndex = Math.min(pageOffset, matches.size());
        int toIndex = Math.min(fromIndex + pageSize, matches.size());
        List<AirportSearchItem> airports = matches.subList(fromIndex, toIndex).stream()
                .map(AirportSearchItem::from)
                .toList();

        return new AirportSearchResponse(
                airports,
                matches.size(),
                fromIndex,
                pageSize,
                toIndex < matches.size()
        );
    }

    List<Airport> rankedMatches(String query) {
        String normalizedQuery = normalize(query);
        if (normalizedQuery.isBlank()) {
            return List.of();
        }

        List<Airport> cached = cachedResult(normalizedQuery);
        if (cached != null) {
            return cached;
        }

        List<AirportMatch> matches = new ArrayList<>();
        for (AirportSearchEntry entry : searchIndex) {
            int score = matchScore(entry, normalizedQuery);
            if (score != NO_MATCH) {
                matches.add(new AirportMatch(entry.airport(), score));
            }
        }
        matches.sort(MATCH_ORDER);
        return cacheResult(normalizedQuery, matches.stream().map(AirportMatch::airport).toList());
    }

    private List<Airport> cachedResult(String normalizedQuery) {
        synchronized (searchCache) {
            return searchCache.get(normalizedQuery);
        }
    }

    private List<Airport> cacheResult(String normalizedQuery, List<Airport> result) {
        synchronized (searchCache) {
            List<Airport> existing = searchCache.get(normalizedQuery);
            if (existing != null) {
                return existing;
            }
            searchCache.put(normalizedQuery, result);
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
                    normalize(airport.iataCode()),
                    List.of(
                            normalize(airport.ident()),
                            normalize(airport.icaoCode()),
                            normalize(airport.gpsCode()),
                            normalize(airport.localCode())
                    ),
                    normalize(airport.name()),
                    normalize(airport.city()),
                    normalize(airport.regionCode()),
                    normalize(airport.country()),
                    normalize(airport.countryCode()),
                    normalize(airport.keywords())
            );
        }
    }
}
