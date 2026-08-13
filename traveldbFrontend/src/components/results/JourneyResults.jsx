import { useTranslation } from "react-i18next";
import BaggageResults from "./BaggageResults";
import DocumentResults from "./DocumentResults";
import {
  actionableBaggageStops,
  documentReviewCount,
  missingInputMessage,
  visibleDocumentRequirements,
} from "./resultHelpers";

export default function JourneyResults({ result, route }) {
  const { t, i18n } = useTranslation();
  const baggageStops = actionableBaggageStops(result.baggageStops);
  const documentRequirements = visibleDocumentRequirements(result.documentCheck?.requirements);
  const documentActions = documentReviewCount(documentRequirements);
  const attentionCount = baggageStops.length + documentActions;

  return (
    <section className="results" aria-labelledby="results-title" aria-live="polite">
      <header className="results-heading">
        <div>
          <span>{t("results.eyebrow")}</span>
          <h2 id="results-title">{t("results.title")}</h2>
        </div>
        {attentionCount > 0 && (
          <strong>{t("results.reviewItems", { count: attentionCount })}</strong>
        )}
      </header>

      <div className="results-grid">
        <BaggageResults route={route} stops={baggageStops} />
        <DocumentResults
          datasetVersion={result.documentCheck?.datasetVersion}
          missingDetails={missingInputMessage(
            result.documentCheck?.missingInputs,
            t,
            i18n.resolvedLanguage,
          )}
          requirements={documentRequirements}
          reviewCount={documentActions}
          route={route}
        />
      </div>

      {!i18n.resolvedLanguage?.startsWith("en") && (
        <p className="result-note guidance-language-note">{t("results.guidanceLanguageNotice")}</p>
      )}
      <p className="result-note">{t("results.finalReminder")}</p>
    </section>
  );
}
