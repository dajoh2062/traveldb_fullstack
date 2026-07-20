package projects.traveldbbackend.documents;

import org.springframework.stereotype.Service;

@Service
public class DocumentRequirementsService {

    private final LocalDocumentRulesProvider localRulesProvider;

    public DocumentRequirementsService(LocalDocumentRulesProvider localRulesProvider) {
        this.localRulesProvider = localRulesProvider;
    }

    public DocumentCheckResult check(DocumentCheckInput input) {
        return localRulesProvider.check(input);
    }
}
