package io.github.dajoh2062.traveldb.documents;

import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

@Service
public class DocumentRulePublicationService {

    private final DocumentRuleRepository repository;
    private final ObjectMapper objectMapper;

    public DocumentRulePublicationService(DocumentRuleRepository repository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public boolean publish(Resource snapshotResource) {
        DocumentRuleSnapshot snapshot = DocumentRuleSnapshotLoader.load(objectMapper, snapshotResource);
        if (repository.findActiveDatasetVersion().filter(snapshot.datasetVersion()::equals).isPresent()) {
            return false;
        }
        repository.saveAndActivate(snapshot);
        return true;
    }
}
