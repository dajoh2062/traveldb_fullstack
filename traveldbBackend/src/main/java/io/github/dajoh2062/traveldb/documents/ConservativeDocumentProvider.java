package io.github.dajoh2062.traveldb.documents;

import org.springframework.stereotype.Component;
import io.github.dajoh2062.traveldb.api.dto.TravelDocument;
import io.github.dajoh2062.traveldb.api.dto.TravelDocument.Type;
import io.github.dajoh2062.traveldb.documents.DocumentRequirement.Category;
import io.github.dajoh2062.traveldb.documents.DocumentRequirement.DocumentSource;
import io.github.dajoh2062.traveldb.documents.DocumentRequirement.Scope;
import io.github.dajoh2062.traveldb.documents.DocumentRequirement.Status;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Component
public class ConservativeDocumentProvider implements DocumentRequirementsProvider {

    private final Clock clock;

    public ConservativeDocumentProvider(Clock clock) {
        this.clock = clock;
    }

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
    public static final DocumentSource ICAO_MACHINE_READABLE_TRAVEL_DOCUMENTS = new DocumentSource(
            "ICAO Doc 9303: Machine Readable Travel Documents",
            "https://www.icao.int/publications/doc-series/doc-9303",
            "INTERNATIONAL_ORGANIZATION"
    );
    public static final DocumentSource UNHCR_TRAVEL_DOCUMENTS = new DocumentSource(
            "UNHCR travel documents guidance",
            "https://help.unhcr.org/global/travel-documents/",
            "INTERNATIONAL_ORGANIZATION"
    );
    public static final DocumentSource ILO_SEAFARER_DOCUMENTS = new DocumentSource(
            "ILO Seafarers' Identity Documents Convention (C185)",
            "https://www.ilo.org/resource/seafarers-identity-documents-convention-revised-2003-amended-no-185",
            "INTERNATIONAL_ORGANIZATION"
    );
    public static final DocumentSource EU_TRAVEL_AND_RESIDENCE_DOCUMENTS = new DocumentSource(
            "European Commission travel and residence documents",
            "https://home-affairs.ec.europa.eu/travel-and-residence-documents_en",
            "GOVERNMENT"
    );
    public static final DocumentSource ICAO_FACILITATION_MANUAL = new DocumentSource(
            "ICAO Facilitation Manual",
            "https://www.icao.int/safety/CAPSCA/PublishingImages/Pages/ICAO-Manuals/9957_cons_en.pdf",
            "INTERNATIONAL_ORGANIZATION"
    );

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
                List.of(IATA_TRAVEL_CENTRE, TRAVELDOC),
                "conservative-travel-document"
        ));

        List<TravelDocument> travelDocuments = safeTravelDocuments(input);
        for (int index = 0; index < travelDocuments.size(); index++) {
            requirements.add(documentAcceptanceRequirement(travelDocuments.get(index), index));
        }

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
                    List.of(IATA_TRAVEL_CENTRE, TRAVELDOC),
                    transit ? "conservative-transit-permission" : "conservative-entry-permission"
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
                        List.of(IATA_TRAVEL_CENTRE, TRAVELDOC),
                        "conservative-entry-conditions"
                )));

        return new DocumentCheckResult(
                "TRAVELDB_CONSERVATIVE",
                false,
                "GLOBAL_VERIFICATION_GUIDANCE",
                Instant.now(clock),
                requirements,
                missingInputs,
                List.of("No licensed live requirements provider is configured. TravelDB has not made a visa-eligibility decision."),
                List.of(IATA_TRAVEL_CENTRE, TRAVELDOC),
                null
        );
    }

    private List<String> missingInputs(DocumentCheckInput input) {
        List<String> missing = new ArrayList<>();
        if (blank(input.residenceCountryCode())) missing.add("Country of residence");
        List<TravelDocument> documents = safeTravelDocuments(input);
        if (documents.isEmpty()) {
            if (blank(input.passportIssuingCountryCode())) missing.add("Passport issuing country");
            if (input.passportExpiryDate() == null) missing.add("Passport expiry date");
        } else {
            documents.stream()
                    .filter(document -> Boolean.TRUE.equals(document.primary()))
                    .findFirst()
                    .filter(document -> document.expiryDate() == null)
                    .ifPresent(document -> missing.add("Primary travel document expiry date"));
        }
        if (input.departureDate() == null) missing.add("Departure date");
        if (blank(input.travelPurpose())) missing.add("Travel purpose");
        return missing;
    }

    private DocumentRequirement documentAcceptanceRequirement(TravelDocument document, int index) {
        Type type = parseType(document.type());
        String label = type == Type.OTHER && !blank(document.customType())
                ? document.customType().trim()
                : type == null ? "travel document" : type.displayName();

        List<String> conditions = new ArrayList<>();
        conditions.add("Registered document type: " + document.type() + ".");
        if (!blank(document.issuingCountryCode())) {
            conditions.add("Issuing country entered: " + document.issuingCountryCode() + ".");
        }
        if (document.expiryDate() != null) {
            conditions.add("Expiry date entered: " + document.expiryDate() + ".");
        }
        if (Boolean.TRUE.equals(document.primary())) {
            conditions.add("This is the primary document selected for the analysis.");
        }
        conditions.add("Confirm acceptance with the border authorities and operating carriers for this route.");

        return new DocumentRequirement(
                "DOCUMENT_ACCEPTANCE_" + (index + 1),
                Category.TRAVEL_DOCUMENT,
                Status.VERIFY,
                Scope.JOURNEY,
                null,
                null,
                "Verify " + label + " acceptance",
                "TravelDB recorded this " + label
                        + " but has not determined that it is accepted. Verify it for every entry and transit point on the route.",
                List.copyOf(conditions),
                documentSources(type),
                "conservative-document-acceptance"
        );
    }

    private List<DocumentSource> documentSources(Type type) {
        List<DocumentSource> sources = new ArrayList<>();
        if (type != null) {
            switch (type) {
                case PASSPORT, DIPLOMATIC_PASSPORT, SERVICE_PASSPORT, OFFICIAL_PASSPORT,
                        MILITARY_PASSPORT, ALIEN_PASSPORT, NATIONAL_ID_CARD,
                        EMERGENCY_TRAVEL_DOCUMENT, LAISSEZ_PASSER ->
                        sources.add(ICAO_MACHINE_READABLE_TRAVEL_DOCUMENTS);
                case REFUGEE_TRAVEL_DOCUMENT, STATELESS_PERSON_TRAVEL_DOCUMENT -> {
                    sources.add(UNHCR_TRAVEL_DOCUMENTS);
                    sources.add(ICAO_MACHINE_READABLE_TRAVEL_DOCUMENTS);
                }
                case RESIDENCE_PERMIT, VISA -> sources.add(EU_TRAVEL_AND_RESIDENCE_DOCUMENTS);
                case SEAFARER_IDENTITY_DOCUMENT -> sources.add(ILO_SEAFARER_DOCUMENTS);
                case CREW_MEMBER_CERTIFICATE -> sources.add(ICAO_FACILITATION_MANUAL);
                case MILITARY_ID, OTHER -> {
                    // These documents do not have one global acceptance regime.
                }
            }
        }
        sources.add(IATA_TRAVEL_CENTRE);
        sources.add(TRAVELDOC);
        return List.copyOf(sources);
    }

    private List<TravelDocument> safeTravelDocuments(DocumentCheckInput input) {
        return input.travelDocuments() == null ? List.of() : input.travelDocuments();
    }

    private Type parseType(String value) {
        try {
            return Type.valueOf(value);
        } catch (IllegalArgumentException | NullPointerException ignored) {
            return null;
        }
    }

    private boolean blank(String value) {
        return value == null || value.isBlank();
    }

}
