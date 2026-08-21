package io.github.dajoh2062.traveldb.documents;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

@Component
public class DocumentRuleDatasetBootstrap implements ApplicationRunner {

    private final DocumentRulePublicationService publicationService;
    private final Resource snapshotResource;
    private final boolean enabled;

    public DocumentRuleDatasetBootstrap(
            DocumentRulePublicationService publicationService,
            @Value("${traveldb.documents.rules-location:classpath:data/document-rules.json}") Resource snapshotResource,
            @Value("${traveldb.documents.bootstrap-enabled:true}") boolean enabled
    ) {
        this.publicationService = publicationService;
        this.snapshotResource = snapshotResource;
        this.enabled = enabled;
    }

    @Override
    public void run(ApplicationArguments arguments) {
        if (!enabled) {
            return;
        }
        publicationService.publish(snapshotResource);
    }
}
