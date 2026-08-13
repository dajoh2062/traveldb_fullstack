import { ArrowDown, ArrowUp, X } from "lucide-react";
import { airportLocation, routeSummary } from "../../utils/journey";
import CountryFlag from "../ui/CountryFlag";

function RouteStop({ airport, index, onMove, onRemove, stopCount }) {
  const isFirst = index === 0;
  const isLast = index === stopCount - 1;
  const role = isFirst ? "Departure" : isLast ? "Destination" : `Transit ${index}`;
  const markerType = isFirst ? "origin" : isLast ? "destination" : "transit";

  return (
    <div className="route-stop">
      <div className="route-rail" aria-hidden="true">
        <span className={`stop-marker ${markerType}`}>{index + 1}</span>
        {!isLast && (
          <span className="route-direction-arrow">
            <ArrowDown aria-hidden="true" className="icon" size={12} strokeWidth={2.2} />
          </span>
        )}
      </div>
      <div className="route-stop-card">
        <div className="route-stop-role">
          <span>{role}</span>
        </div>
        <strong className="route-code">{airport.iataCode}</strong>
        <div className="route-stop-details">
          <strong>{airport.name}</strong>
          <span className="airport-location">
            <CountryFlag countryCode={airport.countryCode} />
            {airportLocation(airport)}
          </span>
        </div>
        <div className="route-stop-actions">
          <button
            aria-label={`Move ${airport.iataCode} earlier in route`}
            className="move-airport"
            disabled={isFirst}
            onClick={() => onMove(airport.iataCode, -1)}
            title="Move earlier"
            type="button"
          >
            <ArrowUp aria-hidden="true" className="icon" size={15} strokeWidth={1.8} />
          </button>
          <button
            aria-label={`Move ${airport.iataCode} later in route`}
            className="move-airport"
            disabled={isLast}
            onClick={() => onMove(airport.iataCode, 1)}
            title="Move later"
            type="button"
          >
            <ArrowDown aria-hidden="true" className="icon" size={15} strokeWidth={1.8} />
          </button>
          <button
            aria-label={`Remove ${airport.iataCode} from route`}
            className="remove-airport"
            onClick={() => onRemove(airport.iataCode)}
            title="Remove airport"
            type="button"
          >
            <X aria-hidden="true" className="icon" size={16} strokeWidth={1.8} />
          </button>
        </div>
      </div>
    </div>
  );
}

export default function RouteTimeline({ error, route, onMove, onRemove }) {
  const className = ["route-builder", route.length === 0 && "is-empty", error && "has-error"]
    .filter(Boolean)
    .join(" ");

  return (
    <div className={className}>
      <div className="route-header">
        <span>Itinerary</span>
        <small>
          {route.length} {route.length === 1 ? "stop" : "stops"}
        </small>
      </div>

      {route.length === 0 ? (
        <p className="empty-route">Add your departure and destination.</p>
      ) : (
        <div className="route-list" aria-label={`Current route: ${routeSummary(route)}`}>
          {route.map((airport, index) => (
            <RouteStop
              airport={airport}
              index={index}
              key={airport.iataCode}
              onMove={onMove}
              onRemove={onRemove}
              stopCount={route.length}
            />
          ))}
        </div>
      )}
      {error && (
        <span className="route-error" role="alert">
          {error}
        </span>
      )}
    </div>
  );
}
