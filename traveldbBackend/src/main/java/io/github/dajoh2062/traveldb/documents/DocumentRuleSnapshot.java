package io.github.dajoh2062.traveldb.documents;

import io.github.dajoh2062.traveldb.documents.DocumentRequirement.DocumentSource;

import java.time.Instant;
import java.util.List;

record DocumentRuleSnapshot(
        String datasetVersion,
        Instant generatedAt,
        List<DocumentSource> sources,
        List<DocumentRule> rules
) {}
