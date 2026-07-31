package projects.traveldbbackend.api.dto;

import projects.traveldbbackend.model.Airport;

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
                airport.getIataCode(),
                airport.getIcaoCode(),
                airport.getName(),
                airport.getCity(),
                airport.getCountry(),
                airport.getCountryCode(),
                airport.isScheduledService()
        );
    }
}
