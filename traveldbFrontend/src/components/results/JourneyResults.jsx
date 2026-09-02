import { useTranslation } from "react-i18next";
import { localizeBaggageAdvice, localizeDocumentRequirement } from "../../i18n/localizedGuidance";
import BaggageResults from "./BaggageResults";
import DocumentResults from "./DocumentResults";
import {
  actionableBaggageStops,
  missingInputMessage,
  sortDocumentRequirementsByCountry,
  visibleDocumentRequirements,
} from "./resultHelpers";

export default function JourneyResults({ result, route }) {
  const { t, i18n } = useTranslation();
  const baggageStops = actionableBaggageStops(result.baggageStops);
  const documentRequirements = sortDocumentRequirementsByCountry(
    visibleDocumentRequirements(result.documentCheck?.requirements),
    route,
  );
  const localizedBaggageStops = baggageStops.map(stop => localizeBaggageAdvice(stop, i18n));
  const localizedDocumentRequirements = documentRequirements.map(requirement =>
    localizeDocumentRequirement(requirement, i18n, result.documentCheck?.datasetVersion),
  );
  const hasEnglishFallback = [...localizedBaggageStops, ...localizedDocumentRequirements].some(
    item => !item.isFullyLocalized,
  );

  return (
    <section className="results" aria-labelledby="results-title" aria-live="polite">
      <header className="results-heading">
        <h2 id="results-title">{t("results.title")}</h2>
      </header>

      <div className="results-grid">
        <BaggageResults route={route} stops={localizedBaggageStops} />
        <DocumentResults
          missingDetails={missingInputMessage(
            result.documentCheck?.missingInputs,
            t,
            i18n.resolvedLanguage,
          )}
          requirements={localizedDocumentRequirements}
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
