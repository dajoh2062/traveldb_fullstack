import AirportSearch from "./AirportSearch";
import BaggageSetup from "./BaggageSetup";
import Icon from "./Icon";
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
  return (
    <section className="planner-card" aria-labelledby="planner-title">
      <div className="section-heading">
        <div>
          <span className="section-label">Journey workspace</span>
          <h2 id="planner-title">Trip details</h2>
        </div>
        <p className="passport-disclaimer" role="note">
          <span className="scope-dot" aria-hidden="true" />
          <span>
            This search is for tourism with a regular (ordinary) passport only. Other passport types
            and travel documents are not supported.
          </span>
        </p>
      </div>
      <form onSubmit={onSubmit} noValidate>
        {countryError && (
          <div className="service-error" role="alert">
            <Icon name="alert" size={18} />
            <span>{countryError}</span>
            <button onClick={onRetryCountries} type="button">
              Try again
            </button>
          </div>
        )}
        <div className="planner-block">
          <div className="planner-block-heading">
            <span className="planner-step">01</span>
            <div>
              <h3>Traveller</h3>
              <p>Passport nationality</p>
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
              <h3>Route</h3>
              <p>From departure to arrival</p>
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
              <Icon name="alert" size={18} />
              <span>{error}</span>
            </div>
          )}
          <button className="primary-button" disabled={isLoading} type="submit">
            {isLoading ? (
              <><span className="spinner" /> Checking...</>
            ) : (
              "Check trip"
            )}
          </button>
        </div>
      </form>
    </section>
  );
}
