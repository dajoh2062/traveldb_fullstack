import AirportSearch from "./AirportSearch";
import BaggageSetup from "./BaggageSetup";
import Icon from "./Icon";
import NationalitySearch from "./NationalitySearch";
import RouteTimeline from "./RouteTimeline";

export default function JourneyPlanner({
  baggage,
  countries,
  error,
  isCountryLoading,
  isLoading,
  nationality,
  nationalityQuery,
  onAddAirport,
  onBaggageChange,
  onNationalityQueryChange,
  onMoveAirport,
  onRemoveAirport,
  onSelectNationality,
  onSubmit,
  route,
}) {
  return (
    <section className="planner-card" aria-labelledby="planner-title">
      <div className="section-heading">
        <div>
          <span className="section-label">Journey details</span>
          <h2 id="planner-title">Passenger and itinerary</h2>
          <p>Add the passport nationality and every airport in travel order.</p>
        </div>
      </div>
      <form onSubmit={onSubmit} noValidate>
        <div className="form-grid">
          <NationalitySearch
            countries={countries}
            isLoading={isCountryLoading}
            nationality={nationality}
            onQueryChange={onNationalityQueryChange}
            onSelect={onSelectNationality}
            query={nationalityQuery}
          />
          <AirportSearch onSelect={onAddAirport} />
        </div>
        <RouteTimeline route={route} onMove={onMoveAirport} onRemove={onRemoveAirport} />
        <BaggageSetup baggage={baggage} onChange={onBaggageChange} />
        {error && (
          <div className="error-message" role="alert"><Icon name="alert" size={19} /><span>{error}</span></div>
        )}
        <button className="primary-button" disabled={isLoading} type="submit">
          {isLoading ? <><span className="spinner" /> Checking your journey…</> : <>Check my journey <Icon name="arrow" size={19} /></>}
        </button>
      </form>
    </section>
  );
}
