package io.github.dajoh2062.traveldb.api;

import java.time.Instant;
import java.util.List;

public record ApiErrorResponse(
        String type,
        String title,
        int status,
        String code,
        String detail,
        String instance,
        Instant timestamp,
        List<InvalidJourneyRequestException.FieldViolation> errors
) {}
