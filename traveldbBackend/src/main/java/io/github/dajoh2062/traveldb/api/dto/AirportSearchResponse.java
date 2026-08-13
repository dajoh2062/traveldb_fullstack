package io.github.dajoh2062.traveldb.api.dto;

import java.util.List;

public record AirportSearchResponse(
        List<AirportSearchItem> airports,
        int total,
        int offset,
        int limit,
        boolean hasMore
) {}
