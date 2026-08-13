package io.github.dajoh2062.traveldb.api.dto;

import io.github.dajoh2062.traveldb.model.Airport;

public record AirportSearchItem(
        String iataCode,
        String icaoCode,
        String name,
        String city,
        String country,
        String countryCode,
        boolean scheduledService
) {
    public static AirportSearchItem from(Airport airport) {
        return new AirportSearchItem(
                airport.iataCode(),
                airport.icaoCode(),
                airport.name(),
                airport.city(),
                airport.country(),
                airport.countryCode(),
                airport.scheduledService()
        );
    }
}
