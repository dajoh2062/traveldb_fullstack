import { TriangleAlert } from "lucide-react";
import { useTranslation } from "react-i18next";
import AirportSearch from "./AirportSearch";
import BaggageSetup from "./BaggageSetup";
import NationalitySearch from "./NationalitySearch";
import RouteTimeline from "./RouteTimeline";

export default function JourneyPlanner({
  baggage,
  countries,
  countryError,
  error,
  fieldErrors = {},
  isCountryLoading,
  isLoading,
  nationality,
  nationalityQuery,
  onAddAirport,
  onBaggageChange,
  onNationalityQueryChange,
  onMoveAirport,
  onRemoveAirport,
  onRetryCountries,
  onSelectNationality,
  onSubmit,
  route,
}) {
  const { t } = useTranslation();

  return (
    <section className="planner-card" aria-labelledby="planner-title">
      <div className="section-heading">
        <div>
          <span className="section-label">{t("planner.eyebrow")}</span>
          <h2 id="planner-title">{t("planner.title")}</h2>
        </div>
        <p className="passport-disclaimer" role="note">
          <span className="scope-dot" aria-hidden="true" />
          <span>{t("planner.passportDisclaimer")}</span>
        </p>
      </div>
      <form onSubmit={onSubmit} noValidate>
        {countryError && (
          <div className="service-error" role="alert">
            <TriangleAlert aria-hidden="true" className="icon" size={18} strokeWidth={1.8} />
            <span>{countryError}</span>
            <button onClick={onRetryCountries} type="button">
              {t("common.retry")}
            </button>
          </div>
        )}
        <div className="planner-block">
          <div className="planner-block-heading">
            <span className="planner-step">01</span>
            <div>
              <h3>{t("planner.traveller.title")}</h3>
              <p>{t("planner.traveller.description")}</p>
            </div>
          </div>
          <div className="planner-block-content">
            <NationalitySearch
              countries={countries}
              error={fieldErrors.nationality}
              isLoading={isCountryLoading}
              nationality={nationality}
              onQueryChange={onNationalityQueryChange}
              onSelect={onSelectNationality}
              query={nationalityQuery}
            />
          </div>
        </div>
        <div className="planner-block">
          <div className="planner-block-heading">
            <span className="planner-step">02</span>
            <div>
              <h3>{t("planner.route.title")}</h3>
              <p>{t("planner.route.description")}</p>
            </div>
          </div>
          <div className="planner-block-content route-block-content">
            <AirportSearch onSelect={onAddAirport} />
            <RouteTimeline
              error={fieldErrors.route}
              onMove={onMoveAirport}
              onRemove={onRemoveAirport}
              route={route}
            />
          </div>
        </div>
        <BaggageSetup baggage={baggage} onChange={onBaggageChange} />
        <div className="form-actions">
          {error && (
            <div className="error-message" id="journey-form-error" role="alert" tabIndex="-1">
              <TriangleAlert aria-hidden="true" className="icon" size={18} strokeWidth={1.8} />
              <span>{error}</span>
            </div>
          )}
          <button className="primary-button" disabled={isLoading} type="submit">
            {isLoading ? (
              <>
                <span className="spinner" /> {t("planner.submitting")}
              </>
            ) : (
              t("planner.submit")
            )}
          </button>
        </div>
      </form>
    </section>
  );
}
