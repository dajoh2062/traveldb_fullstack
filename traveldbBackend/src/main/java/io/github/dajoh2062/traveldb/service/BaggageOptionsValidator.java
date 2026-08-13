package io.github.dajoh2062.traveldb.service;

import org.springframework.stereotype.Component;
import io.github.dajoh2062.traveldb.api.InvalidJourneyRequestException.FieldViolation;
import io.github.dajoh2062.traveldb.api.dto.BaggageOptions;

import java.util.List;
import java.util.Set;

@Component
class BaggageOptionsValidator {

    private static final Set<String> TICKET_ARRANGEMENTS = Set.of(
            "SINGLE_BOOKING", "SEPARATE_TICKETS", "UNKNOWN"
    );
    private static final Set<String> THROUGH_CHECK_STATUSES = Set.of("YES", "NO", "UNKNOWN");

    BaggageOptions validate(BaggageOptions baggage, List<FieldViolation> violations) {
        BaggageOptions options = baggage == null ? BaggageOptions.defaults() : baggage;
        String ticketArrangement = ValidationSupport.validateOption(
                options.ticketArrangement(),
                "baggage.ticketArrangement",
                "UNKNOWN",
                TICKET_ARRANGEMENTS,
                violations
        );
        String checkedThrough = ValidationSupport.validateOption(
                options.checkedThrough(),
                "baggage.checkedThrough",
                "UNKNOWN",
                THROUGH_CHECK_STATUSES,
                violations
        );

        return new BaggageOptions(
                options.checkedBaggage() == null || options.checkedBaggage(),
                ticketArrangement,
                checkedThrough
        );
    }
}
