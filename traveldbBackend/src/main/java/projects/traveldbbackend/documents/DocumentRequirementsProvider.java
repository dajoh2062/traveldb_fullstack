package projects.traveldbbackend.documents;

public interface DocumentRequirementsProvider {
    boolean isAvailable();

    DocumentCheckResult check(DocumentCheckInput input);
}
