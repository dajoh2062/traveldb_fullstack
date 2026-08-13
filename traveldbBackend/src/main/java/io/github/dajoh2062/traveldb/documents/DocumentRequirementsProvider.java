package io.github.dajoh2062.traveldb.documents;

public interface DocumentRequirementsProvider {
    DocumentCheckResult check(DocumentCheckInput input);
}
