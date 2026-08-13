package io.github.dajoh2062.traveldb.baggage;

import io.github.dajoh2062.traveldb.model.Airport;

import java.util.List;

public record BaggageCheckRequest(
        List<Airport> route,
        boolean hasCheckedBaggage,
        TicketArrangement ticketArrangement,
        ThroughCheckStatus throughCheckStatus
) {
    public enum TicketArrangement {
        SINGLE_BOOKING,
        SEPARATE_TICKETS,
        UNKNOWN
    }

    public enum ThroughCheckStatus {
        YES,
        NO,
        UNKNOWN
    }

}
