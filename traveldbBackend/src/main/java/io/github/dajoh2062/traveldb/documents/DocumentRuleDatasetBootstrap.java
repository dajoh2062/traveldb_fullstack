package io.github.dajoh2062.traveldb.documents;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Component
public class DocumentRuleDatasetBootstrap implements ApplicationRunner {

    private final DocumentRuleRepository repository;
    private final ObjectMapper objectMapper;
    private final Resource snapshotResource;
    private final boolean enabled;

    public DocumentRuleDatasetBootstrap(
            DocumentRuleRepository repository,
            ObjectMapper objectMapper,
            @Value("${traveldb.documents.rules-location:classpath:data/document-rules.json}") Resource snapshotResource,
            @Value("${traveldb.documents.bootstrap-enabled:true}") boolean enabled
    ) {
        this.repository = repository;
        this.objectMapper = objectMapper;
        this.snapshotResource = snapshotResource;
        this.enabled = enabled;
    }

    @Override
    public void run(ApplicationArguments arguments) {
        if (!enabled) {
            return;
        }
        DocumentRuleSnapshot snapshot = DocumentRuleSnapshotLoader.load(objectMapper, snapshotResource);
        if (repository.findActive()
                .map(DocumentRuleSnapshot::datasetVersion)
                .filter(snapshot.datasetVersion()::equals)
                .isPresent()) {
            return;
        }
        repository.saveAndActivate(snapshot);
    }
}
