package projects.traveldbbackend.documents;

public interface DocumentRequirementsProvider {
    DocumentCheckResult check(DocumentCheckInput input);
}
