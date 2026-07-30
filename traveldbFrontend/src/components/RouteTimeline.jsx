import { routeSummary } from "../utils/journey";
import Icon from "./Icon";

export default function RouteTimeline({ error, route, onMove, onRemove }) {
  return (
    <div className={`route-builder ${route.length === 0 ? "is-empty" : ""} ${error ? "has-error" : ""}`}>
      <div className="route-header">
        <span>Route</span>
      </div>

      {route.length === 0 ? (
        <p className="empty-route">Add your departure and destination.</p>
      ) : (
        <div className="route-list" aria-label={`Current route: ${routeSummary(route)}`}>
          {route.map((airport, index) => {
            const kind = index === 0 ? "Departure" : index === route.length - 1 ? "Destination" : `Transit ${index}`;
            const markerKind = index === 0 ? "origin" : index === route.length - 1 ? "destination" : "transit";
            return (
              <div className="route-stop" key={airport.iataCode}>
                <div className="route-rail" aria-hidden="true">
                  <span className={`stop-marker ${markerKind}`}>{index + 1}</span>
                  {index < route.length - 1 && (
                    <span className="route-direction-arrow"><Icon name="down" size={12} strokeWidth={2.2} /></span>
                  )}
                </div>
                <div className="route-stop-card">
                  <div className="route-stop-role"><span>{kind}</span></div>
                  <strong className="route-code">{airport.iataCode}</strong>
                  <div className="route-stop-details">
                    <strong>{airport.name}</strong>
                    <span>{airport.city ? `${airport.city} · ${airport.country}` : airport.country}</span>
                  </div>
                  <div className="route-stop-actions">
                    <button
                      aria-label={`Move ${airport.iataCode} earlier in route`}
                      className="move-airport"
                      disabled={index === 0}
                      onClick={() => onMove(airport.iataCode, -1)}
                      title="Move earlier"
                      type="button"
                    >
                      <Icon name="up" size={15} />
                    </button>
                    <button
                      aria-label={`Move ${airport.iataCode} later in route`}
                      className="move-airport"
                      disabled={index === route.length - 1}
                      onClick={() => onMove(airport.iataCode, 1)}
                      title="Move later"
                      type="button"
                    >
                      <Icon name="down" size={15} />
                    </button>
                    <button
                      aria-label={`Remove ${airport.iataCode} from route`}
                      className="remove-airport"
                      onClick={() => onRemove(airport.iataCode)}
                      title="Remove airport"
                      type="button"
                    >
                      <Icon name="close" size={16} />
                    </button>
                  </div>
                </div>
              </div>
            );
          })}
        </div>
      )}
      {error && <span className="route-error" role="alert">{error}</span>}
    </div>
  );
}
