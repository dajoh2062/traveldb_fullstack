import { airportLabel } from "../utils/journey";
import Icon from "./Icon";

const BAGGAGE_STATUS = {
  REQUIRED: { label: "Pick up and recheck", icon: "alert" },
  CONFIRM: { label: "Confirm with airline", icon: "help" },
};

const DOCUMENT_STATUS = {
  REQUIRED: { className: "required", icon: "alert", label: "Required" },
  CONDITIONAL: { className: "conditional", icon: "help", label: "Check" },
  VERIFY: { className: "verify", icon: "help", label: "Verify" },
};

function actionableBaggageStops(stops = []) {
  return stops.filter(stop => stop.status === "REQUIRED" || stop.status === "CONFIRM");
}

function actionableDocumentRequirements(requirements = []) {
  const seen = new Set();

  return requirements.filter(requirement => {
    if (!DOCUMENT_STATUS[requirement.status] || requirement.code === "ENTRY_CONDITIONS") return false;

    const key = documentRequirementKey(requirement);
    if (seen.has(key)) return false;

    seen.add(key);
    return true;
  });
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
  return airportLabel(requirement.airportCode, route);
}

function documentSources(requirement) {
  const sources = requirement.sources ?? [];
  const governmentSources = sources.filter(source => source.sourceType === "GOVERNMENT");
  return governmentSources.length > 0 ? governmentSources : sources.slice(0, 1);
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
  const documentRequirements = actionableDocumentRequirements(result.documentCheck?.requirements);
  const hasMissingDetails = (result.documentCheck?.missingInputs?.length ?? 0) > 0;

  return (
    <section className="results" aria-labelledby="results-title" aria-live="polite">
      <h2 id="results-title">Trip results</h2>

      <div className="simple-results-grid">
        <section className="simple-result-section" aria-labelledby="baggage-results-title">
          <div className="simple-result-heading">
            <Icon name="suitcase" size={20} />
            <h3 id="baggage-results-title">Baggage pickup</h3>
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
                    <div className="document-result-copy">
                      <strong>{stop.title ?? airportLabel(stop.airportCode, route)}</strong>
                      {stop.title && <small>{airportLabel(stop.airportCode, route)}</small>}
                      {stop.explanation && <small>{stop.explanation}</small>}
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
                    <span>
                      <Icon name={status.icon} size={14} /> {status.label}
                    </span>
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
            <Icon name="document" size={20} />
            <h3 id="document-results-title">Travel documents</h3>
          </div>

          {documentRequirements.length > 0 ? (
            <ul className="simple-result-list">
              {documentRequirements.map(document => {
                const status = DOCUMENT_STATUS[document.status];
                const sources = documentSources(document);

                return (
                  <li
                    className={`simple-result-item document ${status.className}`}
                    key={documentRequirementKey(document)}
                  >
                    <div className="document-result-copy">
                      <strong>{document.title}</strong>
                      <small>{documentLocation(document, route)}</small>
                      {document.summary && <small>{document.summary}</small>}
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
                    </div>
                    <span>
                      <Icon name={status.icon} size={14} /> {status.label}
                    </span>
                  </li>
                );
              })}
            </ul>
          ) : (
            <p className="empty-result">
              Requirements could not be confirmed. Check official immigration guidance.
            </p>
          )}
          {hasMissingDetails && (
            <p className="result-note">Use Advanced search for a more precise result.</p>
          )}
        </section>
      </div>

      <p className="result-note">
        Each recommendation links to its supporting authority or carrier guidance. Recheck it before travel.
      </p>
    </section>
  );
}
