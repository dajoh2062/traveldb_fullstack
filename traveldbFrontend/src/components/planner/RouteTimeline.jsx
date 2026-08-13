import { ArrowDown, ArrowUp, X } from "lucide-react";
import { useTranslation } from "react-i18next";
import { routeSummary } from "../../utils/journey";
import { countryDisplayName } from "../../utils/search";
import CountryFlag from "../ui/CountryFlag";

function RouteStop({ airport, index, onMove, onRemove, stopCount }) {
  const { i18n, t } = useTranslation();
  const isFirst = index === 0;
  const isLast = index === stopCount - 1;
  const role = isFirst
    ? t("route.roles.departure")
    : isLast
      ? t("route.roles.destination")
      : t("route.roles.transit", { index });
  const markerType = isFirst ? "origin" : isLast ? "destination" : "transit";
  const countryName = countryDisplayName(
    { countryId: airport.countryCode, countryNameEn: airport.country },
    i18n.resolvedLanguage,
  );
  const location = airport.city ? `${airport.city} · ${countryName}` : countryName;

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
          <strong dir="auto">{airport.name}</strong>
          <span className="airport-location" dir="auto">
            <CountryFlag countryCode={airport.countryCode} />
            {location}
          </span>
        </div>
        <div className="route-stop-actions">
          <button
            aria-label={t("route.actions.moveEarlier", { code: airport.iataCode })}
            className="move-airport"
            disabled={isFirst}
            onClick={() => onMove(airport.iataCode, -1)}
            title={t("route.actionTitles.moveEarlier")}
            type="button"
          >
            <ArrowUp aria-hidden="true" className="icon" size={15} strokeWidth={1.8} />
          </button>
          <button
            aria-label={t("route.actions.moveLater", { code: airport.iataCode })}
            className="move-airport"
            disabled={isLast}
            onClick={() => onMove(airport.iataCode, 1)}
            title={t("route.actionTitles.moveLater")}
            type="button"
          >
            <ArrowDown aria-hidden="true" className="icon" size={15} strokeWidth={1.8} />
          </button>
          <button
            aria-label={t("route.actions.remove", { code: airport.iataCode })}
            className="remove-airport"
            onClick={() => onRemove(airport.iataCode)}
            title={t("route.actionTitles.remove")}
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
  const { t } = useTranslation();
  const className = ["route-builder", route.length === 0 && "is-empty", error && "has-error"]
    .filter(Boolean)
    .join(" ");

  return (
    <div className={className}>
      <div className="route-header">
        <span>{t("route.itinerary")}</span>
        <small>{t("route.stops", { count: route.length })}</small>
      </div>

      {route.length === 0 ? (
        <p className="empty-route">{t("route.empty")}</p>
      ) : (
        <div
          className="route-list"
          aria-label={t("route.currentRoute", { route: routeSummary(route) })}
        >
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
