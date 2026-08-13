package io.github.dajoh2062.traveldb.model;

public record Airport(
        long sourceId,
        String ident,
        String iataCode,
        String icaoCode,
        String gpsCode,
        String localCode,
        String name,
        String city,
        String regionCode,
        String country,
        String countryCode,
        String continent,
        String airportType,
        boolean scheduledService,
        double latitude,
        double longitude,
        Integer elevationFt,
        String officialUrl,
        String wikipediaUrl,
        String keywords,
        boolean schengen
) {}
