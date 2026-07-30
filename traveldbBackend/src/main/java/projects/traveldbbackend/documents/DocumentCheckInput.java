package projects.traveldbbackend.documents;

import projects.traveldbbackend.Airport;

import java.time.LocalDate;
import java.util.List;

public record DocumentCheckInput(
        String nationalityCountryCode,
        List<Airport> route,
        String residenceCountryCode,
        String passportIssuingCountryCode,
        LocalDate passportExpiryDate,
        LocalDate departureDate,
        String travelPurpose,
        Integer travelerAge,
        List<String> residencePermitCountryCodes,
        List<String> visaCountryCodes,
        List<String> entryAirportCodes
) {
    public DocumentCheckInput(
            String nationalityCountryCode,
            List<Airport> route,
            String residenceCountryCode,
            String passportIssuingCountryCode,
            LocalDate passportExpiryDate,
            LocalDate departureDate,
            String travelPurpose,
            Integer travelerAge,
            List<String> residencePermitCountryCodes,
            List<String> visaCountryCodes
    ) {
        this(
                nationalityCountryCode,
                route,
                residenceCountryCode,
                passportIssuingCountryCode,
                passportExpiryDate,
                departureDate,
                travelPurpose,
                travelerAge,
                residencePermitCountryCodes,
                visaCountryCodes,
                List.of()
        );
    }
}
