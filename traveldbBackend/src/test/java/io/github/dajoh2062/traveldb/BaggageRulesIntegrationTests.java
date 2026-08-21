package io.github.dajoh2062.traveldb;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import io.github.dajoh2062.traveldb.api.dto.BaggageOptions;
import io.github.dajoh2062.traveldb.api.dto.JourneyRequest;
import io.github.dajoh2062.traveldb.api.dto.JourneyResponse;
import io.github.dajoh2062.traveldb.baggage.BaggageAdvice;
import io.github.dajoh2062.traveldb.service.JourneyService;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
class BaggageRulesIntegrationTests {

    @Autowired
    private JourneyService journeyService;

    @Test
    void usesConsumerFriendlyBaggageDefaults() {
        BaggageOptions defaults = BaggageOptions.defaults();

        assertTrue(defaults.checkedBaggage());
        assertEquals("SINGLE_BOOKING", defaults.ticketArrangement());
        assertEquals("YES", defaults.checkedThrough());
    }

    @Test
    void listsEveryBaggagePickupInJourneyOrder() {
        JourneyResponse response = journeyService.checkJourney(
                new JourneyRequest(
                        "NO",
                        List.of("OSL", "ATL", "LHR", "JFK", "OSL")
                )
        );

        assertTrue(response.pickupRequired());
        assertEquals(List.of("ATL", "JFK"), response.pickupAt());
        assertEquals("2026-07-31", response.baggageGuidanceReviewed());
    }

    @Test
    void requiresPickupAtSupportedInternationalToDomesticEntryPoints() {
        assertRequiredAt(
                List.of("OSL", "SYD", "MEL"),
                "SYD",
                BaggageAdvice.AdviceCode.AUSTRALIA_INTERNATIONAL_TO_DOMESTIC
        );
        assertRequiredAt(
                List.of("OSL", "AKL", "WLG"),
                "AKL",
                BaggageAdvice.AdviceCode.NEW_ZEALAND_INTERNATIONAL_TO_DOMESTIC
        );
        assertRequiredAt(
                List.of("OSL", "NRT", "CTS"),
                "NRT",
                BaggageAdvice.AdviceCode.JAPAN_INTERNATIONAL_TO_DOMESTIC
        );
        assertRequiredAt(
                List.of("OSL", "DEL", "BOM"),
                "DEL",
                BaggageAdvice.AdviceCode.DELHI_INTERNATIONAL_TO_DOMESTIC
        );
    }

    @Test
    void appliesUsPreclearanceAndRemoteScreeningExceptionsConservatively() {
        JourneyResponse precleared = journeyService.checkJourney(
                request(List.of("DUB", "JFK", "BOS"), "SINGLE_BOOKING", "YES")
        );

        assertFalse(precleared.pickupRequired());
        assertEquals(BaggageAdvice.Status.NOT_REQUIRED, precleared.baggageStops().getFirst().status());
        assertEquals(
                BaggageAdvice.AdviceCode.US_PRECLEARANCE_CHECKED_THROUGH,
                precleared.baggageStops().getFirst().adviceCode()
        );
        assertTrue(precleared.baggageStops().getFirst().title().contains("Precleared"));

        JourneyResponse pilotRoute = journeyService.checkJourney(
                request(List.of("SYD", "LAX", "SFO"), "SINGLE_BOOKING", "YES")
        );
        assertEquals(BaggageAdvice.Status.REQUIRED, pilotRoute.baggageStops().getFirst().status());
        assertEquals(
                BaggageAdvice.AdviceCode.US_SYD_LAX_SCREENING_PILOT,
                pilotRoute.baggageStops().getFirst().adviceCode()
        );
        assertTrue(pilotRoute.baggageStops().getFirst().title().contains("screening pilot"));
    }

    @Test
    void separateTicketsRequireSelfTransferUnlessBagIsConfirmedThrough() {
        JourneyResponse separateTickets = journeyService.checkJourney(
                request(List.of("OSL", "LHR", "SIN"), "SEPARATE_TICKETS", "UNKNOWN")
        );
        assertEquals(List.of("LHR"), separateTickets.pickupAt());
        assertEquals(
                BaggageAdvice.AdviceCode.SEPARATE_TICKETS,
                separateTickets.baggageStops().getFirst().adviceCode()
        );

        JourneyResponse confirmedThrough = journeyService.checkJourney(
                request(List.of("OSL", "LHR", "SIN"), "SEPARATE_TICKETS", "YES")
        );
        assertFalse(confirmedThrough.pickupRequired());
        assertEquals(BaggageAdvice.Status.NOT_REQUIRED, confirmedThrough.baggageStops().getFirst().status());
        assertEquals(
                BaggageAdvice.AdviceCode.CHECKED_THROUGH_NO_KNOWN_RECLAIM,
                confirmedThrough.baggageStops().getFirst().adviceCode()
        );
    }

    @Test
    void marksCanadaAndUnsupportedFirstEntryProcessesForConfirmation() {
        JourneyResponse canada = journeyService.checkJourney(
                request(List.of("OSL", "YYZ", "YVR"), "SINGLE_BOOKING", "UNKNOWN")
        );
        assertEquals(BaggageAdvice.Status.CONFIRM, canada.baggageStops().getFirst().status());
        assertEquals(
                BaggageAdvice.AdviceCode.CANADA_INTERNATIONAL_TO_DOMESTIC,
                canada.baggageStops().getFirst().adviceCode()
        );

        JourneyResponse unsupported = journeyService.checkJourney(
                request(List.of("OSL", "GRU", "GIG"), "SINGLE_BOOKING", "YES")
        );
        assertEquals(BaggageAdvice.Status.CONFIRM, unsupported.baggageStops().getFirst().status());
        assertEquals(
                BaggageAdvice.AdviceCode.GENERIC_INTERNATIONAL_TO_DOMESTIC,
                unsupported.baggageStops().getFirst().adviceCode()
        );
    }

    @Test
    void emitsStableCodesForEveryGeneralAndUsTransferOutcome() {
        assertEquals(
                BaggageAdvice.AdviceCode.NOT_CHECKED_THROUGH,
                adviceCode(List.of("OSL", "LHR", "SIN"), "SINGLE_BOOKING", "NO")
        );
        assertEquals(
                BaggageAdvice.AdviceCode.CHECK_BAGGAGE_TAG,
                adviceCode(List.of("OSL", "LHR", "SIN"), "SINGLE_BOOKING", "UNKNOWN")
        );
        assertEquals(
                BaggageAdvice.AdviceCode.US_PRECLEARANCE_NOT_CHECKED_THROUGH,
                adviceCode(List.of("DUB", "JFK", "BOS"), "SINGLE_BOOKING", "NO")
        );
        assertEquals(
                BaggageAdvice.AdviceCode.US_PRECLEARANCE_CONFIRM_TAG,
                adviceCode(List.of("DUB", "JFK", "BOS"), "SINGLE_BOOKING", "UNKNOWN")
        );
        assertEquals(
                BaggageAdvice.AdviceCode.US_FIRST_ARRIVAL,
                adviceCode(List.of("OSL", "ATL", "BOS"), "SINGLE_BOOKING", "YES")
        );
    }

    @Test
    void skipsBaggageTransferStepsForCarryOnOnlyJourneys() {
        JourneyResponse response = journeyService.checkJourney(
                new JourneyRequest(
                        "NO",
                        List.of("OSL", "ATL", "BOS"),
                        new BaggageOptions(false, "UNKNOWN", "UNKNOWN")
                )
        );

        assertFalse(response.pickupRequired());
        assertTrue(response.baggageStops().isEmpty());
    }

    private void assertRequiredAt(
            List<String> route,
            String airportCode,
            BaggageAdvice.AdviceCode expectedAdviceCode
    ) {
        JourneyResponse response = journeyService.checkJourney(
                request(route, "SINGLE_BOOKING", "YES")
        );
        assertEquals(List.of(airportCode), response.pickupAt());
        assertEquals(BaggageAdvice.Status.REQUIRED, response.baggageStops().getFirst().status());
        assertEquals(expectedAdviceCode, response.baggageStops().getFirst().adviceCode());
        assertFalse(response.baggageStops().getFirst().sources().isEmpty());
    }

    private JourneyRequest request(
            List<String> route,
            String ticketArrangement,
            String checkedThrough
    ) {
        return new JourneyRequest(
                "NO",
                route,
                new BaggageOptions(true, ticketArrangement, checkedThrough)
        );
    }

    private BaggageAdvice.AdviceCode adviceCode(
            List<String> route,
            String ticketArrangement,
            String checkedThrough
    ) {
        return journeyService.checkJourney(request(route, ticketArrangement, checkedThrough))
                .baggageStops()
                .getFirst()
                .adviceCode();
    }
}
