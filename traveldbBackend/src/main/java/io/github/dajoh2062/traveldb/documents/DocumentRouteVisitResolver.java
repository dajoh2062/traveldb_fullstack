package io.github.dajoh2062.traveldb.documents;

import io.github.dajoh2062.traveldb.model.Airport;
import io.github.dajoh2062.traveldb.documents.DocumentRequirement.Scope;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

final class DocumentRouteVisitResolver {

    private DocumentRouteVisitResolver() {}

    static List<CountryVisit> resolve(List<Airport> route) {
        return resolve(route, List.of());
    }

    static List<CountryVisit> resolve(List<Airport> route, List<String> entryAirportCodes) {
        if (route == null || route.size() < 2) return List.of();

        List<CountryVisit> visits = new ArrayList<>();
        Set<String> forcedEntryAirports = new LinkedHashSet<>();
        if (entryAirportCodes != null) {
            entryAirportCodes.stream()
                    .filter(code -> code != null && !code.isBlank())
                    .map(code -> code.trim().toUpperCase(Locale.ROOT))
                    .forEach(forcedEntryAirports::add);
        }
        int runStart = 0;
        while (runStart < route.size()) {
            String jurisdictionCode = jurisdictionCode(route.get(runStart));
            int runEnd = runStart;
            while (runEnd + 1 < route.size()
                    && jurisdictionCode.equals(jurisdictionCode(route.get(runEnd + 1)))) {
                runEnd++;
            }

            // A trip that starts inside Schengen can still cross a national
            // border without crossing the Schengen external border. Emit the
            // first foreign airport so its reviewed travel-document rule is
            // not lost (for example OSL -> CDG).
            if (runStart == 0 && "SCHENGEN".equals(jurisdictionCode)) {
                String originCountry = countryCode(route.getFirst());
                for (int index = 1; index <= runEnd; index++) {
                    Airport airport = route.get(index);
                    if (!originCountry.equals(countryCode(airport))) {
                        visits.add(new CountryVisit(countryCode(airport), airport.iataCode(), Scope.ENTRY));
                        break;
                    }
                }
            }

            // For every later jurisdiction run, immigration is encountered at
            // its first airport. A final run is entry. An intermediate run is
            // also entry when it contains an onward domestic/intra-Schengen
            // flight, because the traveller must cross the border first.
            if (runStart > 0) {
                Airport firstArrival = route.get(runStart);
                boolean crossesBorder = runEnd == route.size() - 1
                        || runEnd > runStart
                        || forcedEntryAirports.contains(firstArrival.iataCode());
                Scope scope = crossesBorder ? Scope.ENTRY : Scope.TRANSIT;
                visits.add(new CountryVisit(countryCode(firstArrival), firstArrival.iataCode(), scope));
            }

            runStart = runEnd + 1;
        }

        return List.copyOf(visits);
    }

    private static String countryCode(Airport airport) {
        if (airport == null || airport.countryCode() == null) return "";
        return airport.countryCode().trim().toUpperCase(Locale.ROOT);
    }

    private static String jurisdictionCode(Airport airport) {
        return airport != null && airport.schengen() ? "SCHENGEN" : countryCode(airport);
    }

    record CountryVisit(String countryCode, String airportCode, Scope scope) {}
}
