package io.github.dajoh2062.traveldb.baggage;

import io.github.dajoh2062.traveldb.baggage.BaggageAdvice.AdviceCode;
import io.github.dajoh2062.traveldb.baggage.BaggageAdvice.Source;
import io.github.dajoh2062.traveldb.baggage.BaggageAdvice.Status;
import io.github.dajoh2062.traveldb.baggage.BaggageCheckRequest.ThroughCheckStatus;
import io.github.dajoh2062.traveldb.baggage.BaggageCheckRequest.TicketArrangement;

import java.util.List;

record BaggageRule(
        String id,
        int position,
        int priority,
        Boolean enteringCountry,
        Boolean onwardDomestic,
        String currentCountryCode,
        String currentAirportCode,
        String previousAirportCode,
        String previousAirportGroup,
        TicketArrangement ticketArrangement,
        ThroughCheckStatus throughCheckStatus,
        AdviceCode adviceCode,
        Status status,
        String title,
        String explanation,
        List<String> exceptions,
        List<Source> sources
) {}
