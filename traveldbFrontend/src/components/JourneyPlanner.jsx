import AirportSearch from "./AirportSearch";
import BaggageSetup from "./BaggageSetup";
import Icon from "./Icon";
import NationalitySearch from "./NationalitySearch";
import RouteTimeline from "./RouteTimeline";
import TravelDocuments from "./TravelDocuments";

export default function JourneyPlanner({
  advancedSearch,
  baggage,
  countries,
  countryError,
  documents,
  error,
  fieldErrors = {},
  isCountryLoading,
  isLoading,
  nationality,
  nationalityQuery,
  onAddAirport,
  onAdvancedSearchChange,
  onBaggageChange,
  onDocumentChange,
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
        <label className="advanced-search-toggle">
          <input
            aria-controls="advanced-document-fields"
            aria-describedby="advanced-search-description"
            aria-label="Advanced search"
            checked={advancedSearch}
            onChange={event => onAdvancedSearchChange(event.target.checked)}
            type="checkbox"
          />
          <span>
            <strong>Advanced search</strong>
            <small id="advanced-search-description">
              Add passport and traveller details.
            </small>
          </span>
        </label>
        <div hidden={!advancedSearch} id="advanced-document-fields">
          <TravelDocuments
            countries={countries}
            documents={documents}
            errors={fieldErrors}
            onChange={onDocumentChange}
          />
        </div>
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
