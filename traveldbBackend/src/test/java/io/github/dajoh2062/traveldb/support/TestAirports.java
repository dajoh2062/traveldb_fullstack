package io.github.dajoh2062.traveldb.support;

import io.github.dajoh2062.traveldb.model.Airport;

public final class TestAirports {

    private TestAirports() {}

    public static Airport airport(String iataCode, String countryCode) {
        return airport(iataCode, countryCode, false);
    }

    public static Airport airport(String iataCode, String countryCode, boolean schengen) {
        return new Airport(
                0,
                iataCode,
                iataCode,
                null,
                null,
                null,
                iataCode,
                null,
                null,
                countryCode,
                countryCode,
                null,
                "test_airport",
                false,
                0,
                0,
                null,
                null,
                null,
                null,
                schengen
        );
    }
}
