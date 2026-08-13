package io.github.dajoh2062.traveldb.api.dto;

import java.time.LocalDate;
import java.util.List;

public record DocumentOptions(
        String residenceCountryCode,
        String passportIssuingCountryCode,
        LocalDate passportExpiryDate,
        LocalDate departureDate,
        String travelPurpose,
        Integer travelerAge,
        List<String> residencePermitCountryCodes,
        List<String> visaCountryCodes,
        List<TravelDocument> travelDocuments
) {
    public DocumentOptions(
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

    public static DocumentOptions defaults() {
        return new DocumentOptions(null, null, null, null, null, null, List.of(), List.of(), List.of());
    }
}
