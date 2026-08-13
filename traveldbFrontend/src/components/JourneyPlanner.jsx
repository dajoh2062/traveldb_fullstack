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
        <h2 id="planner-title">Trip details</h2>
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
        <div className="form-grid">
          <NationalitySearch
            countries={countries}
            error={fieldErrors.nationality}
            isLoading={isCountryLoading}
            nationality={nationality}
            onQueryChange={onNationalityQueryChange}
            onSelect={onSelectNationality}
            query={nationalityQuery}
          />
          <AirportSearch onSelect={onAddAirport} />
        </div>
        <RouteTimeline
          error={fieldErrors.route}
          onMove={onMoveAirport}
          onRemove={onRemoveAirport}
          route={route}
        />
        <p className="passport-disclaimer" role="note">
          <Icon name="document" size={18} />
          <span>
            This search is for tourism with a regular (ordinary) passport only. Other passport types
            and travel documents are not supported.
          </span>
        </p>
        <BaggageSetup baggage={baggage} onChange={onBaggageChange} />
        {error && (
          <div className="error-message" id="journey-form-error" role="alert" tabIndex="-1">
            <Icon name="alert" size={19} />
            <span>{error}</span>
          </div>
        )}
        <button className="primary-button" disabled={isLoading} type="submit">
          {isLoading ? (
            <><span className="spinner" /> Checking...</>
          ) : (
            <>Check trip <Icon name="arrow" size={19} /></>
          )}
        </button>
      </form>
    </section>
  );
}
