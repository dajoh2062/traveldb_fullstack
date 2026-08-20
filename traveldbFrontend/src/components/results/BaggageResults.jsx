import { CircleHelp, ExternalLink, Luggage, TriangleAlert } from "lucide-react";
import { useTranslation } from "react-i18next";
import { airportLabel } from "../../utils/journey";
import { baggageSources } from "./resultHelpers";

const BAGGAGE_STATUS = {
  REQUIRED: { labelKey: "results.baggage.statuses.required", Icon: TriangleAlert },
  CONFIRM: { labelKey: "results.baggage.statuses.confirm", Icon: CircleHelp },
};

export default function BaggageResults({ route, stops }) {
  const { t } = useTranslation();

  return (
    <section className="result-section" aria-labelledby="baggage-results-title">
      <div className="result-section-heading">
        <span className="result-heading-icon">
          <Luggage aria-hidden="true" className="icon" size={22} strokeWidth={1.8} />
        </span>
        <div>
          <h3 id="baggage-results-title">{t("results.baggage.title")}</h3>
          <small>
            {stops.length > 0
              ? t("results.baggage.actions", { count: stops.length })
              : t("results.baggage.noAction")}
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
                  {t(status.labelKey)}
                </span>
                <div className="result-content">
                  <strong dir="auto" lang={stop.title ? stop.localized.title.lang : undefined}>
                    {stop.title ? stop.localized.title.text : airportLabel(stop.airportCode, route)}
                  </strong>
                  {stop.title && (
                    <small className="result-location">
                      {airportLabel(stop.airportCode, route)}
                    </small>
                  )}
                  {stop.explanation && (
                    <p dir="auto" lang={stop.localized.explanation.lang}>
                      {stop.localized.explanation.text}
                    </p>
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
                            {source.label ?? t("results.baggage.sourceFallback")}
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
                </div>
              </li>
            );
          })}
        </ol>
      ) : (
        <p className="empty-result">{t("results.baggage.empty")}</p>
      )}
    </section>
  );
}
