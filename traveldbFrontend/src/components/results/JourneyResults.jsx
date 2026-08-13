import BaggageResults from "./BaggageResults";
import DocumentResults from "./DocumentResults";
import {
  actionableBaggageStops,
  documentReviewCount,
  missingInputMessage,
  visibleDocumentRequirements,
} from "./resultHelpers";

export default function JourneyResults({ result, route }) {
  const baggageStops = actionableBaggageStops(result.baggageStops);
  const documentRequirements = visibleDocumentRequirements(result.documentCheck?.requirements);
  const documentActions = documentReviewCount(documentRequirements);
  const attentionCount = baggageStops.length + documentActions;

  return (
    <section className="results" aria-labelledby="results-title" aria-live="polite">
      <header className="results-heading">
        <div>
          <span>Trip results</span>
          <h2 id="results-title">What you need to do</h2>
        </div>
        {attentionCount > 0 && (
          <strong>
            {attentionCount} {attentionCount === 1 ? "item" : "items"} to review
          </strong>
        )}
      </header>

      <div className="results-grid">
        <BaggageResults route={route} stops={baggageStops} />
        <DocumentResults
          datasetVersion={result.documentCheck?.datasetVersion}
          missingDetails={missingInputMessage(result.documentCheck?.missingInputs)}
          requirements={documentRequirements}
          reviewCount={documentActions}
          route={route}
        />
      </div>

      <p className="result-note">Recheck official guidance before travel.</p>
    </section>
  );
}
