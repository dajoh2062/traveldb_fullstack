import { Check, CircleHelp, ExternalLink, FileText, TriangleAlert } from "lucide-react";
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
    label: "Required before travel",
  },
  CONDITIONAL: {
    className: "conditional",
    Icon: CircleHelp,
    label: "Required unless exempt",
  },
  VERIFY: {
    className: "verify",
    Icon: CircleHelp,
    label: "Could not confirm",
  },
  NOT_REQUIRED: {
    className: "not-required",
    Icon: Check,
    label: "Not required",
  },
};

export default function DocumentResults({
  datasetVersion,
  missingDetails,
  requirements,
  reviewCount,
  route,
}) {
  return (
    <section className="result-section" aria-labelledby="document-results-title">
      <div className="result-section-heading">
        <span className="result-heading-icon">
          <FileText aria-hidden="true" className="icon" size={22} strokeWidth={1.8} />
        </span>
        <div>
          <h3 id="document-results-title">Travel documents</h3>
          <small>
            {requirements.length === 0
              ? "Not confirmed"
              : reviewCount > 0
                ? `${reviewCount} to review`
                : "No action needed"}
          </small>
        </div>
      </div>

      {requirements.length > 0 ? (
        <ul className="result-list">
          {requirements.map(document => {
            const status = DOCUMENT_STATUS[document.status];
            const StatusIcon = status.Icon;
            const sources = documentSources(document);
            const keyFacts = documentKeyFacts(document);
            const conditions = documentConditions(document);
            const visibleConditions = conditions.slice(0, 2);
            const additionalConditions = conditions.slice(2);
            const lastVerified = formatRuleDate(document.lastVerified);

            return (
              <li
                className={`result-item document ${status.className}`}
                key={documentRequirementKey(document)}
              >
                <span className="result-status">
                  <StatusIcon aria-hidden="true" className="icon" size={15} strokeWidth={1.8} />
                  {status.label}
                </span>
                <div className="result-content">
                  <strong>{document.title}</strong>
                  <small className="result-location">{documentLocation(document, route)}</small>
                  {document.summary && <p>{document.summary}</p>}
                  {keyFacts.length > 0 && (
                    <dl className="document-key-facts">
                      {keyFacts.map(fact => (
                        <div className="document-key-fact" key={`${fact.label}:${fact.value}`}>
                          <dt>{fact.label}</dt>
                          <dd>{fact.value}</dd>
                        </div>
                      ))}
                    </dl>
                  )}
                  {visibleConditions.length > 0 && (
                    <div className="document-conditions" aria-label="Important conditions">
                      {visibleConditions.map(condition => (
                        <p className="document-condition" key={condition}>
                          {condition}
                        </p>
                      ))}
                    </div>
                  )}
                  {additionalConditions.length > 0 && (
                    <details className="document-more-conditions">
                      <summary>
                        {additionalConditions.length} more{" "}
                        {additionalConditions.length === 1 ? "condition" : "conditions"}
                      </summary>
                      <div className="document-conditions">
                        {additionalConditions.map(condition => (
                          <p className="document-condition" key={condition}>
                            {condition}
                          </p>
                        ))}
                      </div>
                    </details>
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
                          {sourceLabel(source, sources.length)}
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
                    <small className="document-rule-meta">Rule verified {lastVerified}</small>
                  )}
                </div>
              </li>
            );
          })}
        </ul>
      ) : (
        <p className="empty-result">
          Requirements could not be confirmed. Check official immigration guidance.
        </p>
      )}
      {datasetVersion && <p className="document-dataset">Local rule set {datasetVersion}</p>}
      {missingDetails && <p className="result-note missing-input-note">{missingDetails}</p>}
    </section>
  );
}
