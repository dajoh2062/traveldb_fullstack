import { Check, CircleHelp, ExternalLink, FileText, TriangleAlert } from "lucide-react";
import { useTranslation } from "react-i18next";
import {
  documentConditions,
  documentKeyFacts,
  documentLocation,
  documentRequirementKey,
  documentSources,
  formatRuleDate,
  sourceLabel,
} from "./resultHelpers";

const DOCUMENT_STATUS = {
  REQUIRED: {
    className: "required",
    Icon: TriangleAlert,
    labelKey: "results.documents.statuses.required",
  },
  CONDITIONAL: {
    className: "conditional",
    Icon: CircleHelp,
    labelKey: "results.documents.statuses.conditional",
  },
  VERIFY: {
    className: "verify",
    Icon: CircleHelp,
    labelKey: "results.documents.statuses.verify",
  },
  NOT_REQUIRED: {
    className: "not-required",
    Icon: Check,
    labelKey: "results.documents.statuses.notRequired",
  },
};

export default function DocumentResults({
  datasetVersion,
  missingDetails,
  requirements,
  reviewCount,
  route,
}) {
  const { t, i18n } = useTranslation();

  return (
    <section className="result-section" aria-labelledby="document-results-title">
      <div className="result-section-heading">
        <span className="result-heading-icon">
          <FileText aria-hidden="true" className="icon" size={22} strokeWidth={1.8} />
        </span>
        <div>
          <h3 id="document-results-title">{t("results.documents.title")}</h3>
          <small>
            {requirements.length === 0
              ? t("results.documents.notConfirmed")
              : reviewCount > 0
                ? t("results.documents.reviewCount", { count: reviewCount })
                : t("results.documents.noAction")}
          </small>
        </div>
      </div>

      {requirements.length > 0 ? (
        <ul className="result-list">
          {requirements.map(document => {
            const status = DOCUMENT_STATUS[document.status];
            const StatusIcon = status.Icon;
            const sources = documentSources(document);
            const keyFacts = (document.keyFacts ?? [])
              .map((fact, index) => ({ fact, localized: document.localized.keyFacts[index] }))
              .filter(({ fact }) => documentKeyFacts({ keyFacts: [fact] }).length > 0)
              .slice(0, 6)
              .map(({ fact, localized }) => ({
                label: localized?.label ?? { text: fact.label, lang: "en" },
                value: localized?.value ?? { text: fact.value, lang: "en" },
              }));
            const conditions = (document.conditions ?? [])
              .map((condition, index) => ({
                condition,
                localized: document.localized.conditions[index],
              }))
              .filter(({ condition }) => documentConditions({ conditions: [condition] }).length > 0)
              .map(({ condition, localized }) => localized ?? { text: condition, lang: "en" });
            const lastVerified = formatRuleDate(document.lastVerified, i18n.resolvedLanguage);

            return (
              <li
                className={`result-item document ${status.className}`}
                key={documentRequirementKey(document)}
              >
                <span className="result-status">
                  <StatusIcon aria-hidden="true" className="icon" size={15} strokeWidth={1.8} />
                  {t(status.labelKey)}
                </span>
                <div className="result-content">
                  <strong dir="auto" lang={document.localized.title.lang}>
                    {document.localized.title.text}
                  </strong>
                  <small className="result-location">{documentLocation(document, route, t)}</small>
                  {document.summary && (
                    <p dir="auto" lang={document.localized.summary.lang}>
                      {document.localized.summary.text}
                    </p>
                  )}
                  {(keyFacts.length > 0 ||
                    conditions.length > 0 ||
                    sources.length > 0 ||
                    lastVerified) && (
                    <details className="document-more-conditions">
                      <summary>{t("results.documents.viewMore")}</summary>
                      {keyFacts.length > 0 && (
                        <dl className="document-key-facts" dir="auto">
                          {keyFacts.map(fact => (
                            <div
                              className="document-key-fact"
                              key={`${fact.label.text}:${fact.value.text}`}
                            >
                              <dt lang={fact.label.lang}>{fact.label.text}</dt>
                              <dd lang={fact.value.lang}>{fact.value.text}</dd>
                            </div>
                          ))}
                        </dl>
                      )}
                      {conditions.length > 0 && (
                        <div
                          className="document-conditions"
                          aria-label={t("results.documents.importantConditions")}
                          dir="auto"
                        >
                          {conditions.map(condition => (
                            <p
                              className="document-condition"
                              key={condition.text}
                              lang={condition.lang}
                            >
                              {condition.text}
                            </p>
                          ))}
                        </div>
                      )}
                      {sources.length > 0 && (
                        <div className="document-source-links">
                          {sources.map(source => (
                            <a
                              href={source.url}
                              key={source.url}
                              rel="noopener noreferrer"
                              target="_blank"
                            >
                              <span dir="auto" lang={source.label ? "en" : undefined}>
                                {sourceLabel(source, sources.length, t)}
                              </span>
                              <ExternalLink
                                aria-hidden="true"
                                className="icon"
                                size={12}
                                strokeWidth={1.8}
                              />
                            </a>
                          ))}
                        </div>
                      )}
                      {lastVerified && (
                        <small className="document-rule-meta">
                          {t("results.documents.verifiedDate", { date: lastVerified })}
                        </small>
                      )}
                    </details>
                  )}
                </div>
              </li>
            );
          })}
        </ul>
      ) : (
        <p className="empty-result">{t("results.documents.empty")}</p>
      )}
      {datasetVersion && (
        <p className="document-dataset">
          {t("results.documents.dataset", { version: datasetVersion })}
        </p>
      )}
      {missingDetails && <p className="result-note missing-input-note">{missingDetails}</p>}
    </section>
  );
}
