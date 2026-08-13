package io.github.dajoh2062.traveldb.documents;

import io.github.dajoh2062.traveldb.documents.DocumentRequirement.DocumentSource;

import java.util.List;

record DocumentRuleSnapshot(
        String datasetVersion,
        List<DocumentSource> sources,
        List<DocumentRule> rules
) {}
