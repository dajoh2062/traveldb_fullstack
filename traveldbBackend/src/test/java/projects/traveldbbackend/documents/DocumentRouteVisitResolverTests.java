package projects.traveldbbackend.documents;

import org.junit.jupiter.api.Test;
import projects.traveldbbackend.model.Airport;
import projects.traveldbbackend.documents.DocumentRequirement.Scope;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DocumentRouteVisitResolverTests {

    @Test
    void placesEntryAtTheFirstAirportInTheFinalCountry() {
        assertEquals(
                List.of(
                        new DocumentRouteVisitResolver.CountryVisit("US", "JFK", Scope.TRANSIT),
                        new DocumentRouteVisitResolver.CountryVisit("AU", "BNE", Scope.ENTRY)
                ),
                DocumentRouteVisitResolver.resolve(List.of(
                        airport("OSL", "NO"),
                        airport("JFK", "US"),
                        airport("BNE", "AU"),
                        airport("MEL", "AU")
                ))
        );
    }

    @Test
    void collapsesDomesticConnectionsIntoOneBorderVisit() {
        assertEquals(
                List.of(new DocumentRouteVisitResolver.CountryVisit("US", "JFK", Scope.ENTRY)),
                DocumentRouteVisitResolver.resolve(List.of(
                        airport("OSL", "NO"),
                        airport("JFK", "US"),
                        airport("BOS", "US")
                ))
        );
    }

    @Test
    void distinguishesTrueTransitFromFinalCountryEntry() {
        assertEquals(
                List.of(
                        new DocumentRouteVisitResolver.CountryVisit("AU", "SYD", Scope.TRANSIT),
                        new DocumentRouteVisitResolver.CountryVisit("NZ", "AKL", Scope.ENTRY)
                ),
                DocumentRouteVisitResolver.resolve(List.of(
                        airport("OSL", "NO"),
                        airport("SYD", "AU"),
                        airport("AKL", "NZ")
                ))
        );
    }

    @Test
    void treatsBaggageCollectionAtATransitStopAsEntry() {
        assertEquals(
                List.of(
                        new DocumentRouteVisitResolver.CountryVisit("AU", "SYD", Scope.ENTRY),
                        new DocumentRouteVisitResolver.CountryVisit("NZ", "AKL", Scope.ENTRY)
                ),
                DocumentRouteVisitResolver.resolve(
                        List.of(
                                airport("OSL", "NO"),
                                airport("SYD", "AU"),
                                airport("AKL", "NZ")
                        ),
                        List.of("SYD")
                )
        );
    }

    @Test
    void treatsAnIntermediateDomesticLegAsEntryAtItsFirstAirport() {
        assertEquals(
                List.of(
                        new DocumentRouteVisitResolver.CountryVisit("AU", "SYD", Scope.ENTRY),
                        new DocumentRouteVisitResolver.CountryVisit("NZ", "AKL", Scope.ENTRY)
                ),
                DocumentRouteVisitResolver.resolve(List.of(
                        airport("OSL", "NO"),
                        airport("SYD", "AU"),
                        airport("MEL", "AU"),
                        airport("AKL", "NZ")
                ))
        );
    }

    @Test
    void treatsAnIntermediateSchengenLegAsEntryAtItsFirstAirport() {
        assertEquals(
                List.of(
                        new DocumentRouteVisitResolver.CountryVisit("DE", "FRA", Scope.ENTRY),
                        new DocumentRouteVisitResolver.CountryVisit("GB", "LHR", Scope.ENTRY)
                ),
                DocumentRouteVisitResolver.resolve(List.of(
                        airport("JFK", "US", false),
                        airport("FRA", "DE", true),
                        airport("CDG", "FR", true),
                        airport("LHR", "GB", false)
                ))
        );
    }

    @Test
    void placesSchengenEntryAtTheFirstExternalBorderAirport() {
        assertEquals(
                List.of(new DocumentRouteVisitResolver.CountryVisit("DE", "FRA", Scope.ENTRY)),
                DocumentRouteVisitResolver.resolve(List.of(
                        airport("JFK", "US", false),
                        airport("FRA", "DE", true),
                        airport("CDG", "FR", true)
                ))
        );
    }

    @Test
    void keepsTheFirstForeignCountryOnAnIntraSchengenJourney() {
        assertEquals(
                List.of(new DocumentRouteVisitResolver.CountryVisit("FR", "CDG", Scope.ENTRY)),
                DocumentRouteVisitResolver.resolve(List.of(
                        airport("OSL", "NO", true),
                        airport("CDG", "FR", true)
                ))
        );
    }

    private Airport airport(String iataCode, String countryCode) {
        return airport(iataCode, countryCode, "NO".equals(countryCode));
    }

    private Airport airport(String iataCode, String countryCode, boolean schengen) {
        Airport airport = new Airport();
        airport.setIataCode(iataCode);
        airport.setCountryCode(countryCode);
        airport.setCountry(countryCode);
        airport.setSchengen(schengen);
        return airport;
    }
}
