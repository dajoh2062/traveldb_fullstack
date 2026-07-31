package projects.traveldbbackend.rules;

import projects.traveldbbackend.model.Airport;

import java.util.List;

public record RuleContext(
        String nationality,
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
