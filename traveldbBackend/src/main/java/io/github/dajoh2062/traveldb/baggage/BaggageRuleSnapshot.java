package io.github.dajoh2062.traveldb.baggage;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;

record BaggageRuleSnapshot(
        String datasetVersion,
        LocalDate reviewedDate,
        Map<String, Set<String>> airportGroups,
        List<BaggageRule> rules
) {}
