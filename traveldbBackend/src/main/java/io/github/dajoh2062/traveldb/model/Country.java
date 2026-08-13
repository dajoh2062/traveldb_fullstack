package io.github.dajoh2062.traveldb.model;

public record Country(
        long sourceId,
        String countryId,
        String countryNameEn,
        String continent,
        String wikipediaUrl,
        String keywords,
        boolean schengen
) {}
