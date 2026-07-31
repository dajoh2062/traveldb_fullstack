package projects.traveldbbackend.api.dto;

import java.time.LocalDate;

/**
 * A document the traveler plans to carry.
 *
 * <p>The type remains a string at the API boundary so unsupported values can be
 * reported as ordinary field-validation errors instead of JSON parse errors.</p>
 */
public record TravelDocument(
        String type,
        String customType,
        String issuingCountryCode,
        LocalDate expiryDate,
        Boolean primary
) {
    public enum Type {
        PASSPORT("passport", true),
        DIPLOMATIC_PASSPORT("diplomatic passport", true),
        SERVICE_PASSPORT("service passport", true),
        OFFICIAL_PASSPORT("official passport", true),
        MILITARY_PASSPORT("military passport", true),
        ALIEN_PASSPORT("alien passport", true),
        NATIONAL_ID_CARD("national identity card", true),
        REFUGEE_TRAVEL_DOCUMENT("refugee travel document", true),
        STATELESS_PERSON_TRAVEL_DOCUMENT("stateless person travel document", true),
        EMERGENCY_TRAVEL_DOCUMENT("emergency travel document", true),
        LAISSEZ_PASSER("laissez-passer", false),
        RESIDENCE_PERMIT("residence permit", true),
        VISA("visa", true),
        SEAFARER_IDENTITY_DOCUMENT("seafarer's identity document", true),
        CREW_MEMBER_CERTIFICATE("crew member certificate", true),
        MILITARY_ID("military identity document", true),
        OTHER("other travel document", false);

        private final String displayName;
        private final boolean issuingCountryRequired;

        Type(String displayName, boolean issuingCountryRequired) {
            this.displayName = displayName;
            this.issuingCountryRequired = issuingCountryRequired;
        }

        public String displayName() {
            return displayName;
        }

        public boolean issuingCountryRequired() {
            return issuingCountryRequired;
        }
    }
}
