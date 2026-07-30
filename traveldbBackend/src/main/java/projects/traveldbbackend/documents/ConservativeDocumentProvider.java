package projects.traveldbbackend.documents;

import org.springframework.stereotype.Component;
import projects.traveldbbackend.documents.DocumentRequirement.Category;
import projects.traveldbbackend.documents.DocumentRequirement.DocumentSource;
import projects.traveldbbackend.documents.DocumentRequirement.Scope;
import projects.traveldbbackend.documents.DocumentRequirement.Status;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Component
public class ConservativeDocumentProvider implements DocumentRequirementsProvider {

    public static final DocumentSource IATA_TRAVEL_CENTRE = new DocumentSource(
            "IATA Travel Centre (Timatic)",
            "https://www.iatatravelcentre.com/",
            "AUTHORITATIVE_INDUSTRY"
    );
    public static final DocumentSource TRAVELDOC = new DocumentSource(
            "TravelDoc global requirements checker",
            "https://library.traveldoc.aero/",
            "AUTHORITATIVE_INDUSTRY"
    );

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public DocumentCheckResult check(DocumentCheckInput input) {
        List<DocumentRequirement> requirements = new ArrayList<>();
        List<String> missingInputs = missingInputs(input);

        requirements.add(new DocumentRequirement(
                "TRAVEL_DOCUMENT",
                Category.TRAVEL_DOCUMENT,
                Status.VERIFY,
                Scope.JOURNEY,
                null,
                null,
                "Passport or accepted travel document",
                "Confirm which travel document is accepted and its minimum remaining validity. National identity cards can replace passports on some regional journeys.",
                List.of("Check validity on both the arrival date and planned departure date.", "Verify blank-page and document-condition requirements."),
                List.of(IATA_TRAVEL_CENTRE, TRAVELDOC)
        ));

        List<DocumentRouteVisitResolver.CountryVisit> visits = DocumentRouteVisitResolver.resolve(
                input.route(),
                input.entryAirportCodes()
        );
        for (DocumentRouteVisitResolver.CountryVisit visit : visits) {
            boolean transit = visit.scope() == Scope.TRANSIT;
            requirements.add(new DocumentRequirement(
                    transit ? "TRANSIT_PERMISSION" : "ENTRY_PERMISSION",
                    transit ? Category.TRANSIT_PERMISSION : Category.VISA,
                    Status.VERIFY,
                    visit.scope(),
                    visit.countryCode(),
                    visit.airportCode(),
                    transit ? "Transit permission" : "Visa, eVisa or electronic authorisation",
                    transit
                            ? "Transit rules depend on nationality, connection length, airport transfer route and whether border control is crossed."
                            : "Entry permission depends on nationality, residence, purpose, stay length and documents already held.",
                    transit
                            ? List.of("Airside and landside connections can have different rules.", "Airport or terminal changes usually require entry permission.")
                            : List.of("Residence permits and valid visas from other countries can create exemptions.", "Visa-free entry can still require an ETA or arrival registration."),
                    List.of(IATA_TRAVEL_CENTRE, TRAVELDOC)
            ));
        }

        visits.stream()
                .filter(visit -> visit.scope() == Scope.ENTRY)
                .forEach(entry -> requirements.add(new DocumentRequirement(
                        "ENTRY_CONDITIONS",
                        Category.OTHER,
                        Status.VERIFY,
                        Scope.ENTRY,
                        entry.countryCode(),
                        entry.airportCode(),
                        "Additional entry evidence",
                        "Border authorities may request onward travel, accommodation details, sufficient funds, insurance, health documents or declarations.",
                        List.of("Rules can differ for minors and non-tourist travel.", "Check whether an online arrival or customs form must be completed before departure."),
                        List.of(IATA_TRAVEL_CENTRE, TRAVELDOC)
                )));

        return new DocumentCheckResult(
                "TRAVELDB_CONSERVATIVE",
                false,
                "GLOBAL_VERIFICATION_GUIDANCE",
                Instant.now(),
                requirements,
                missingInputs,
                List.of("No licensed live requirements provider is configured. TravelDB has not made a visa-eligibility decision."),
                List.of(IATA_TRAVEL_CENTRE, TRAVELDOC)
        );
    }

    private List<String> missingInputs(DocumentCheckInput input) {
        List<String> missing = new ArrayList<>();
        if (blank(input.residenceCountryCode())) missing.add("Country of residence");
        if (blank(input.passportIssuingCountryCode())) missing.add("Passport issuing country");
        if (input.passportExpiryDate() == null) missing.add("Passport expiry date");
        if (input.departureDate() == null) missing.add("Departure date");
        if (blank(input.travelPurpose())) missing.add("Travel purpose");
        return missing;
    }

    private boolean blank(String value) {
        return value == null || value.isBlank();
    }

}
