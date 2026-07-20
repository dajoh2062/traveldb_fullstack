import { airportLabel, routeSummary } from "../utils/journey";
import Icon from "./Icon";

const baggageStatusDetails = {
  REQUIRED: { label: "Collect and recheck", icon: "alert" },
  CONFIRM: { label: "Confirm with airline", icon: "help" },
  NOT_REQUIRED: { label: "No known pickup", icon: "check" },
};

const documentStatusDetails = {
  REQUIRED: { label: "Required", icon: "alert" },
  NOT_REQUIRED: { label: "Not required", icon: "check" },
  CONDITIONAL: { label: "Conditional", icon: "help" },
  VERIFY: { label: "Verify", icon: "search" },
};

function baggageHeadline(checkedBaggage, requiredCount, confirmCount) {
  if (!checkedBaggage) return "No checked baggage steps";
  if (requiredCount > 0) {
    const pickupText = `${requiredCount} ${requiredCount === 1 ? "pickup" : "pickups"} required`;
    return confirmCount > 0 ? `${pickupText}, ${confirmCount} to confirm` : pickupText;
  }
  if (confirmCount > 0) return `${confirmCount} ${confirmCount === 1 ? "stop" : "stops"} to confirm`;
  return "No known transit pickup requirement";
}

function documentHeadline(documentCheck) {
  const requirements = documentCheck?.requirements ?? [];
  const required = requirements.filter(item => item.status === "REQUIRED").length;
  const unresolved = requirements.filter(item => ["CONDITIONAL", "VERIFY"].includes(item.status)).length;
  if (required > 0) return `${required} confirmed ${required === 1 ? "requirement" : "requirements"}`;
  if (unresolved > 0) return `${unresolved} ${unresolved === 1 ? "item" : "items"} to verify`;
  if (requirements.some(item => item.status === "NOT_REQUIRED")) return "No listed permission required";
  return "Document check unavailable";
}

function BaggageStop({ advice, index, route }) {
  const status = baggageStatusDetails[advice.status] ?? baggageStatusDetails.CONFIRM;

  return (
    <li className={`baggage-advice ${advice.status.toLowerCase()}`}>
      <div className="baggage-advice-topline">
        <span className="advice-order">{index + 1}</span>
        <div className="advice-airport">
          <strong>{airportLabel(advice.airportCode, route)}</strong>
          <span>Transit stop</span>
        </div>
        <span className="advice-status"><Icon name={status.icon} size={14} /> {status.label}</span>
      </div>
      <div className="baggage-advice-copy">
        <h4>{advice.title}</h4>
        <p>{advice.explanation}</p>
        {advice.exceptions?.length > 0 && (
          <details>
            <summary>Exceptions and important conditions</summary>
            <ul>{advice.exceptions.map(exception => <li key={exception}>{exception}</li>)}</ul>
          </details>
        )}
        <SourceLinks sources={advice.sources} />
      </div>
    </li>
  );
}

function SourceLinks({ sources }) {
  if (!sources?.length) return null;
  return (
    <div className="official-sources" aria-label="Verification sources">
      <span><Icon name="verified" size={14} /> Sources</span>
      <div>
        {sources.map(source => (
          <a href={source.url} key={source.url} rel="noreferrer" target="_blank">
            {source.label}<Icon name="external" size={12} />
          </a>
        ))}
      </div>
    </div>
  );
}

function requirementLocation(requirement, route) {
  if (requirement.scope === "JOURNEY") return "Entire journey";
  const airport = route.find(item => item.iataCode === requirement.airportCode)
    ?? route.find(item => item.countryCode === requirement.countryCode);
  const location = airport?.country ?? requirement.countryCode;
  return requirement.scope === "TRANSIT" ? `${location} transit` : `${location} entry`;
}

function DocumentRequirementCard({ requirement, index, route }) {
  const status = documentStatusDetails[requirement.status] ?? documentStatusDetails.VERIFY;
  return (
    <li className={`document-advice ${requirement.status.toLowerCase()}`}>
      <div className="document-advice-topline">
        <span className="advice-order">{index + 1}</span>
        <div className="advice-airport">
          <strong>{requirement.title}</strong>
          <span>{requirementLocation(requirement, route)}</span>
        </div>
        <span className="advice-status"><Icon name={status.icon} size={14} /> {status.label}</span>
      </div>
      <div className="document-advice-copy">
        <p>{requirement.summary}</p>
        {requirement.conditions?.length > 0 && (
          <details>
            <summary>Conditions and exceptions</summary>
            <ul>{requirement.conditions.map(condition => <li key={condition}>{condition}</li>)}</ul>
          </details>
        )}
        <SourceLinks sources={requirement.sources} />
      </div>
    </li>
  );
}

export default function JourneyResults({ baggage, nationality, nationalityQuery, result, route }) {
  const baggageStops = result.baggageStops ?? [];
  const baggageRequiredCount = baggageStops.filter(stop => stop.status === "REQUIRED").length;
  const baggageConfirmCount = baggageStops.filter(stop => stop.status === "CONFIRM").length;
  const baggagePanelState = baggageRequiredCount > 0 ? "attention" : baggageConfirmCount > 0 ? "conditional" : "clear";
  const documentCheck = result.documentCheck;
  const usesLocalRules = documentCheck?.provider === "TRAVELDB_LOCAL_RULES";
  const documentRequirements = documentCheck?.requirements ?? [];
  const documentRequiredCount = documentRequirements.filter(item => item.status === "REQUIRED").length;
  const documentUnresolvedCount = documentRequirements.filter(item => ["CONDITIONAL", "VERIFY"].includes(item.status)).length;
  const documentPanelState = documentRequiredCount > 0
    ? "attention"
    : documentUnresolvedCount > 0
      ? "conditional"
      : "clear";

  return (
    <section className="results" aria-labelledby="results-title" aria-live="polite">
      <div className="results-header">
        <div>
          <span className="results-kicker"><Icon name="check" size={15} strokeWidth={2.4} /> Check complete</span>
          <h2 id="results-title">Journey requirements</h2>
          <p>{routeSummary(route)} · Passport nationality: {nationalityQuery} ({nationality})</p>
        </div>
      </div>
      <div className="result-grid">
        <article className={`result-panel baggage-panel ${baggagePanelState}`}>
          <div className="result-icon"><Icon name="suitcase" size={23} /></div>
          <div className="result-copy">
            <span className="panel-label">Checked baggage</span>
            <h3>{baggageHeadline(baggage.checkedBaggage, baggageRequiredCount, baggageConfirmCount)}</h3>
            <p>
              {!baggage.checkedBaggage
                ? "This itinerary was checked for carry-on baggage only."
                : baggageStops.length > 0
                  ? "Review every connection below. Customs requirements override a through-checked baggage tag."
                  : "There are no transit airports in this itinerary."}
            </p>
          </div>
        </article>
        <article className={`result-panel documents-panel ${documentPanelState}`}>
          <div className="result-icon"><Icon name="document" size={23} /></div>
          <div className="result-copy">
            <span className="panel-label">Travel documents</span>
            <h3>{documentHeadline(documentCheck)}</h3>
            <p>
              {usesLocalRules
                ? "TravelDB evaluated the locally stored rule snapshot for this traveller and every stop."
                : documentCheck?.liveData
                ? "Live global requirements were checked for the supplied traveller and itinerary."
                : "Review each unresolved item with the cited authorities before travel."}
            </p>
            <span className={`provider-badge ${usesLocalRules || documentCheck?.liveData ? "live" : "verification"}`}>
              <Icon name={usesLocalRules || documentCheck?.liveData ? "verified" : "help"} size={13} />
              {usesLocalRules ? "Offline rule engine" : documentCheck?.liveData ? "Live provider" : "Verification guidance"}
            </span>
          </div>
        </article>
      </div>

      {baggageStops.length > 0 && (
        <section className="baggage-breakdown" aria-labelledby="baggage-breakdown-title">
          <div className="baggage-breakdown-heading">
            <div>
              <span className="section-label">Connection by connection</span>
              <h3 id="baggage-breakdown-title">Baggage transfer details</h3>
            </div>
            <span className="guidance-date">Guidance reviewed {result.baggageGuidanceReviewed}</span>
          </div>
          <ol className="baggage-advice-list">
            {baggageStops.map((advice, index) => (
              <BaggageStop advice={advice} index={index} key={`${advice.airportCode}-${index}`} route={route} />
            ))}
          </ol>
        </section>
      )}

      <section className="document-breakdown" aria-labelledby="document-breakdown-title">
        <div className="baggage-breakdown-heading">
          <div>
            <span className="section-label">Passport, visa and entry rules</span>
            <h3 id="document-breakdown-title">Travel document details</h3>
          </div>
          <span className="guidance-date">Checked {documentCheck?.checkedAt?.slice(0, 10)}</span>
        </div>

        {(documentCheck?.warnings?.length > 0 || documentCheck?.missingInputs?.length > 0) && (
          <div className="document-warning">
            <Icon name="info" size={18} />
            <div>
              {documentCheck.warnings?.map(warning => <p key={warning}>{warning}</p>)}
              {documentCheck.missingInputs?.length > 0 && (
                <p><strong>Missing details:</strong> {documentCheck.missingInputs.join(", ")}.</p>
              )}
            </div>
          </div>
        )}

        <ol className="document-advice-list">
          {documentRequirements.map((requirement, index) => (
            <DocumentRequirementCard
              index={index}
              key={`${requirement.code}-${requirement.countryCode ?? "journey"}-${index}`}
              requirement={requirement}
              route={route}
            />
          ))}
        </ol>
      </section>

      <div className="disclaimer">
        <Icon name="info" size={18} />
        <div>
          <p><strong>Before you travel</strong> Requirements can change and possession of documents does not guarantee admission. Verify with the operating airline and official border authorities.</p>
          {result.notes?.length > 0 && <ul>{result.notes.map(note => <li key={note}>{note}</li>)}</ul>}
        </div>
      </div>
    </section>
  );
}
