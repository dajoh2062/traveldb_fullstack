import { CircleHelp, ExternalLink, Luggage, TriangleAlert } from "lucide-react";
import { airportLabel } from "../../utils/journey";
import { baggageSources } from "./resultHelpers";

const BAGGAGE_STATUS = {
  REQUIRED: { label: "Pick up and recheck", Icon: TriangleAlert },
  CONFIRM: { label: "Confirm with airline", Icon: CircleHelp },
};

export default function BaggageResults({ route, stops }) {
  return (
    <section className="result-section" aria-labelledby="baggage-results-title">
      <div className="result-section-heading">
        <span className="result-heading-icon">
          <Luggage aria-hidden="true" className="icon" size={22} strokeWidth={1.8} />
        </span>
        <div>
          <h3 id="baggage-results-title">Baggage</h3>
          <small>
            {stops.length > 0
              ? `${stops.length} ${stops.length === 1 ? "action" : "actions"}`
              : "No action found"}
          </small>
        </div>
      </div>

      {stops.length > 0 ? (
        <ol className="result-list">
          {stops.map((stop, index) => {
            const status = BAGGAGE_STATUS[stop.status];
            const StatusIcon = status.Icon;
            const sources = baggageSources(stop);

            return (
              <li
                className={`result-item ${stop.status.toLowerCase()}`}
                key={`${stop.airportCode}-${index}`}
              >
                <span className="result-status">
                  <StatusIcon aria-hidden="true" className="icon" size={15} strokeWidth={1.8} />
                  {status.label}
                </span>
                <div className="result-content">
                  <strong>{stop.title ?? airportLabel(stop.airportCode, route)}</strong>
                  {stop.title && (
                    <small className="result-location">
                      {airportLabel(stop.airportCode, route)}
                    </small>
                  )}
                  {stop.explanation && <p>{stop.explanation}</p>}
                  {sources.length > 0 && (
                    <div className="document-source-links">
                      {sources.map(source => (
                        <a
                          href={source.url}
                          key={source.url}
                          rel="noopener noreferrer"
                          target="_blank"
                        >
                          {source.label ?? "Baggage guidance"}
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
                </div>
              </li>
            );
          })}
        </ol>
      ) : (
        <p className="empty-result">No baggage pickup identified.</p>
      )}
    </section>
  );
}
