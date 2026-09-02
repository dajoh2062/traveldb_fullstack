import { Check, CircleHelp, FileText, TriangleAlert } from "lucide-react";
import { useTranslation } from "react-i18next";
import {
  documentConditions,
  documentCountryName,
  documentKeyFacts,
  documentLocation,
  documentRequirementKey,
  groupDocumentRequirementsByCountry,
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

export default function DocumentResults({ missingDetails, requirements, route }) {
  const { t, i18n } = useTranslation();
  const countryGroups = groupDocumentRequirementsByCountry(requirements, route);

  return (
    <section className="result-section" aria-labelledby="document-results-title">
      <div className="result-section-heading">
        <span className="result-heading-icon">
          <FileText aria-hidden="true" className="icon" size={22} strokeWidth={1.8} />
        </span>
        <h3 id="document-results-title">{t("results.documents.title")}</h3>
      </div>

      {requirements.length > 0 ? (
        <div className="document-country-groups">
          {countryGroups.map(group => {
            const countryName =
              documentCountryName(group.countryCode, i18n.resolvedLanguage) ??
              t("results.documents.locations.wholeJourney");

            return (
              <section
                className="document-country"
                aria-labelledby={`document-country-${group.countryCode || "journey"}`}
                key={group.countryCode || "journey"}
              >
                <h4 id={`document-country-${group.countryCode || "journey"}`}>{countryName}</h4>
                <ul className="result-list">
                  {group.requirements.map(document => {
                    const status = DOCUMENT_STATUS[document.status];
                    const StatusIcon = status.Icon;
                    const keyFacts = (document.keyFacts ?? [])
                      .map((fact, index) => ({
                        fact,
                        localized: document.localized.keyFacts[index],
                      }))
                      .filter(({ fact }) => documentKeyFacts({ keyFacts: [fact] }).length > 0)
                      .slice(0, 4)
                      .map(({ fact, localized }) => ({
                        label: localized?.label ?? { text: fact.label, lang: "en" },
                        value: localized?.value ?? { text: fact.value, lang: "en" },
                      }));
                    const conditions = (document.conditions ?? [])
                      .map((condition, index) => ({
                        condition,
                        localized: document.localized.conditions[index],
                      }))
                      .filter(
                        ({ condition }) =>
                          documentConditions({ conditions: [condition] }).length > 0,
                      )
                      .slice(0, 3)
                      .map(
                        ({ condition, localized }) => localized ?? { text: condition, lang: "en" },
                      );

                    return (
                      <li
                        className={`result-item document ${status.className}`}
                        key={`${group.countryCode}:${documentRequirementKey(document)}`}
                      >
                        <span className="result-status">
                          <StatusIcon
                            aria-hidden="true"
                            className="icon"
                            size={15}
                            strokeWidth={1.8}
                          />
                          {t(status.labelKey)}
                        </span>
                        <div className="result-content">
                          <strong dir="auto" lang={document.localized.title.lang}>
                            {document.localized.title.text}
                          </strong>
                          {document.scope !== "JOURNEY" && (
                            <small className="result-location">
                              {documentLocation(document, route, t)}
                            </small>
                          )}
                          {document.summary && (
                            <p dir="auto" lang={document.localized.summary.lang}>
                              {document.localized.summary.text}
                            </p>
                          )}
                          {(keyFacts.length > 0 || conditions.length > 0) && (
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
                            </details>
                          )}
                        </div>
                      </li>
                    );
                  })}
                </ul>
              </section>
            );
          })}
        </div>
      ) : (
        <p className="empty-result">{t("results.documents.empty")}</p>
      )}
      {missingDetails && <p className="result-note missing-input-note">{missingDetails}</p>}
    </section>
  );
}
