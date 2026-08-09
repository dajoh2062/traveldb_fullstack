import { airportLabel } from "../utils/journey";
import Icon from "./Icon";

const BAGGAGE_STATUS = {
  REQUIRED: { label: "Pick up and recheck", icon: "alert" },
  CONFIRM: { label: "Confirm with airline", icon: "help" },
};

const DOCUMENT_STATUS = {
  REQUIRED: { className: "required", icon: "alert", label: "Required before travel" },
  CONDITIONAL: { className: "conditional", icon: "help", label: "Required unless exempt" },
  VERIFY: { className: "verify", icon: "help", label: "Could not confirm" },
  NOT_REQUIRED: { className: "not-required", icon: "check", label: "Not required" },
};

const INTERNAL_CONDITION = /^Local rule .+ was last verified \d{4}-\d{2}-\d{2}\.?$/i;
const MONTH_NAMES = ["Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"];

function actionableBaggageStops(stops = []) {
  return stops.filter(stop => stop.status === "REQUIRED" || stop.status === "CONFIRM");
}

function visibleDocumentRequirements(requirements = []) {
  const seen = new Set();

  return requirements.filter(requirement => {
    if (!DOCUMENT_STATUS[requirement.status] || requirement.code === "ENTRY_CONDITIONS") return false;

    const key = documentRequirementKey(requirement);
    if (seen.has(key)) return false;

    seen.add(key);
    return true;
  });
}

function documentReviewCount(requirements) {
  return requirements.filter(requirement => requirement.status !== "NOT_REQUIRED").length;
}

function documentRequirementKey(requirement) {
  return [
    requirement.code ?? requirement.title,
    requirement.scope ?? "JOURNEY",
    requirement.airportCode ?? requirement.countryCode ?? "ALL",
  ].join(":");
}

function documentLocation(requirement, route) {
  if (!requirement.airportCode) return "Whole journey";
  const prefix = requirement.scope === "TRANSIT"
    ? "Transit at"
    : requirement.scope === "ENTRY" ? "Entry at" : null;
  const location = airportLabel(requirement.airportCode, route);
  return prefix ? `${prefix} ${location}` : location;
}

function documentSources(requirement) {
  const sources = requirement.sources ?? [];
  const governmentSources = sources.filter(source => source.sourceType === "GOVERNMENT");
  return (governmentSources.length > 0 ? governmentSources : sources).slice(0, 2);
}

function documentKeyFacts(requirement) {
  return (requirement.keyFacts ?? []).filter(fact => (
    typeof fact?.label === "string"
    && fact.label.trim()
    && typeof fact?.value === "string"
    && fact.value.trim()
  )).slice(0, 6);
}

function documentConditions(requirement) {
  return (requirement.conditions ?? []).filter(condition => (
    typeof condition === "string"
    && condition.trim()
    && !INTERNAL_CONDITION.test(condition.trim())
  ));
}

function formatRuleDate(value) {
  const match = /^(\d{4})-(\d{2})-(\d{2})$/.exec(value ?? "");
  if (!match) return null;

  const month = Number(match[2]);
  if (month < 1 || month > 12) return null;
  return `${Number(match[3])} ${MONTH_NAMES[month - 1]} ${match[1]}`;
}

function missingInputMessage(missingInputs = []) {
  const inputs = [...new Set(missingInputs
    .filter(input => typeof input === "string" && input.trim())
    .map(input => `${input.trim().charAt(0).toLowerCase()}${input.trim().slice(1)}`))];
  if (inputs.length === 0) return null;

  const formattedInputs = new Intl.ListFormat("en", {
    style: "long",
    type: "conjunction",
  }).format(inputs);
  return `Add ${formattedInputs} in Advanced search for a more precise result.`;
}

function baggageSources(stop) {
  return (stop.sources ?? []).slice(0, 2);
}

function sourceLabel(source, sourceCount) {
  if (source.label?.includes("eVisitor")) return "eVisitor 651";
  if (source.label?.includes("Electronic Travel Authority")) return "ETA 601";
  if (source.label) return source.label;
  return sourceCount === 1 ? "Supporting source" : "Source";
}

export default function JourneyResults({ result, route }) {
  const baggageStops = actionableBaggageStops(result.baggageStops);
  const documentRequirements = visibleDocumentRequirements(result.documentCheck?.requirements);
  const documentActions = documentReviewCount(documentRequirements);
  const missingDetails = missingInputMessage(result.documentCheck?.missingInputs);
  const datasetVersion = result.documentCheck?.datasetVersion;
  const attentionCount = baggageStops.length + documentActions;

  return (
    <section className="results" aria-labelledby="results-title" aria-live="polite">
      <header className="results-heading">
        <div>
          <span>Trip results</span>
          <h2 id="results-title">What you need to do</h2>
        </div>
        {attentionCount > 0 && (
          <strong>{attentionCount} {attentionCount === 1 ? "item" : "items"} to review</strong>
        )}
      </header>

      <div className="simple-results-grid">
        <section className="simple-result-section" aria-labelledby="baggage-results-title">
          <div className="simple-result-heading">
            <span className="result-heading-icon"><Icon name="suitcase" size={22} /></span>
            <div>
              <h3 id="baggage-results-title">Baggage</h3>
              <small>{baggageStops.length > 0 ? `${baggageStops.length} ${baggageStops.length === 1 ? "action" : "actions"}` : "No action found"}</small>
            </div>
          </div>

          {baggageStops.length > 0 ? (
            <ol className="simple-result-list">
              {baggageStops.map((stop, index) => {
                const status = BAGGAGE_STATUS[stop.status];
                const sources = baggageSources(stop);
                return (
                  <li
                    className={`simple-result-item ${stop.status.toLowerCase()}`}
                    key={`${stop.airportCode}-${index}`}
                  >
                    <span className="result-status">
                      <Icon name={status.icon} size={15} /> {status.label}
                    </span>
                    <div className="document-result-copy">
                      <strong>{stop.title ?? airportLabel(stop.airportCode, route)}</strong>
                      {stop.title && <small className="result-location">{airportLabel(stop.airportCode, route)}</small>}
                      {stop.explanation && <p>{stop.explanation}</p>}
                      {sources.length > 0 && (
                        <div className="document-source-links">
                          {sources.map(source => (
                            <a href={source.url} key={source.url} rel="noopener noreferrer" target="_blank">
                              {source.label ?? "Baggage guidance"}
                              <Icon name="external" size={12} />
                            </a>
                          ))}
                        </div>
                      )}
                    </div>
                  </li>
                );
              })}
            </ol>
          ) : (
            <p className="empty-result">No baggage pickup identified.</p>
          )}
        </section>

        <section className="simple-result-section" aria-labelledby="document-results-title">
          <div className="simple-result-heading">
            <span className="result-heading-icon"><Icon name="document" size={22} /></span>
            <div>
              <h3 id="document-results-title">Travel documents</h3>
              <small>
                {documentRequirements.length === 0
                  ? "Not confirmed"
                  : documentActions > 0 ? `${documentActions} to review` : "No action needed"}
              </small>
            </div>
          </div>

          {documentRequirements.length > 0 ? (
            <ul className="simple-result-list">
              {documentRequirements.map(document => {
                const status = DOCUMENT_STATUS[document.status];
                const sources = documentSources(document);
                const keyFacts = documentKeyFacts(document);
                const conditions = documentConditions(document);
                const visibleConditions = conditions.slice(0, 2);
                const additionalConditions = conditions.slice(2);
                const lastVerified = formatRuleDate(document.lastVerified);

                return (
                  <li
                    className={`simple-result-item document ${status.className}`}
                    key={documentRequirementKey(document)}
                  >
                    <span className="result-status">
                      <Icon name={status.icon} size={15} /> {status.label}
                    </span>
                    <div className="document-result-copy">
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
                            <p className="document-condition" key={condition}>{condition}</p>
                          ))}
                        </div>
                      )}
                      {additionalConditions.length > 0 && (
                        <details className="document-more-conditions">
                          <summary>
                            {additionalConditions.length} more {additionalConditions.length === 1 ? "condition" : "conditions"}
                          </summary>
                          <div className="document-conditions">
                            {additionalConditions.map(condition => (
                              <p className="document-condition" key={condition}>{condition}</p>
                            ))}
                          </div>
                        </details>
                      )}
                      {sources.length > 0 && (
                        <div className="document-source-links">
                          {sources.map(source => (
                            <a href={source.url} key={source.url} rel="noopener noreferrer" target="_blank">
                              {sourceLabel(source, sources.length)}
                              <Icon name="external" size={12} />
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
          {datasetVersion && (
            <p className="document-dataset">Local rule set {datasetVersion}</p>
          )}
          {missingDetails && (
            <p className="result-note missing-input-note">{missingDetails}</p>
          )}
        </section>
      </div>

      <p className="result-note">Check the linked guidance again before travel.</p>
    </section>
  );
}
