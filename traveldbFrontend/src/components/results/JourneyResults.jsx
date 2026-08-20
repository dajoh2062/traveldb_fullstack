import { useTranslation } from "react-i18next";
import { localizeBaggageAdvice, localizeDocumentRequirement } from "../../i18n/localizedGuidance";
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
  const localizedBaggageStops = baggageStops.map(stop => localizeBaggageAdvice(stop, i18n));
  const localizedDocumentRequirements = documentRequirements.map(requirement =>
    localizeDocumentRequirement(requirement, i18n, result.documentCheck?.datasetVersion),
  );
  const documentActions = documentReviewCount(documentRequirements);
  const attentionCount = baggageStops.length + documentActions;
  const hasEnglishFallback = [...localizedBaggageStops, ...localizedDocumentRequirements].some(
    item => !item.isFullyLocalized,
  );

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
        <BaggageResults route={route} stops={localizedBaggageStops} />
        <DocumentResults
          datasetVersion={result.documentCheck?.datasetVersion}
          missingDetails={missingInputMessage(
            result.documentCheck?.missingInputs,
            t,
            i18n.resolvedLanguage,
          )}
          requirements={localizedDocumentRequirements}
          reviewCount={documentActions}
          route={route}
        />
      </div>

      {!i18n.resolvedLanguage?.startsWith("en") && hasEnglishFallback && (
        <p className="result-note guidance-language-note">{t("results.guidanceLanguageNotice")}</p>
      )}
      <p className="result-note">{t("results.finalReminder")}</p>
    </section>
  );
}
