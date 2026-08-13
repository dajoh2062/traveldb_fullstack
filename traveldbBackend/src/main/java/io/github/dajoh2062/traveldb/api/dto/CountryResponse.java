package io.github.dajoh2062.traveldb.api.dto;

import io.github.dajoh2062.traveldb.model.Country;

public record CountryResponse(
        long sourceId,
        String countryId,
        String countryNameEn,
        String continent,
        String wikipediaUrl,
        String keywords,
        boolean schengen
) {
    public static CountryResponse from(Country country) {
        return new CountryResponse(
                country.sourceId(),
                country.countryId(),
                country.countryNameEn(),
                country.continent(),
                country.wikipediaUrl(),
                country.keywords(),
                country.schengen()
        );
    }
}
