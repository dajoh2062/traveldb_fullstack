import useAirportSearch from "../hooks/useAirportSearch";
import Icon from "./Icon";

export default function AirportSearch({ onSelect }) {
  const {
    clearSearch,
    hasMore,
    isLoadingMore,
    isOpen,
    isSearching,
    loadMore,
    query,
    setIsOpen,
    suggestions,
    total,
    updateQuery,
  } = useAirportSearch();

  function selectAirport(airport) {
    if (onSelect(airport) !== false) clearSearch();
  }

  return (
    <div
      className="field airport-field"
      onBlur={event => {
        if (!event.currentTarget.contains(event.relatedTarget)) setIsOpen(false);
      }}
    >
      <label className="field-label" htmlFor="airport-search">Add an airport</label>
      <span className="input-shell">
        <Icon name="search" size={20} />
        <input
          aria-autocomplete="list"
          aria-controls="airport-suggestions"
          aria-expanded={isOpen}
          autoComplete="off"
          id="airport-search"
          onChange={event => updateQuery(event.target.value)}
          onFocus={() => { if (query.trim()) setIsOpen(true); }}
          onKeyDown={event => {
            if (event.key === "Enter" && suggestions.length > 0) {
              event.preventDefault();
              selectAirport(suggestions[0]);
            }
            if (event.key === "Escape") setIsOpen(false);
          }}
          placeholder="City, airport, IATA code, or country"
          role="combobox"
          value={query}
        />
        {isSearching && <Icon name="loader" size={17} />}
      </span>
      <small>IATA code matches are ranked first</small>
      {isOpen && query.trim() && (
        <div className="dropdown airport-dropdown">
          <div className="airport-dropdown-results" id="airport-suggestions" role="listbox">
            {isSearching && suggestions.length === 0 && (
              <div className="dropdown-status"><Icon name="loader" size={16} /> Searching airports…</div>
            )}
            {!isSearching && suggestions.length === 0 && <div className="dropdown-status">No airports found</div>}
            {suggestions.map(airport => (
              <button
                className="dropdown-item"
                key={airport.iataCode}
                onClick={() => selectAirport(airport)}
                role="option"
                type="button"
              >
                <span className="airport-code">{airport.iataCode}</span>
                <span className="airport-meta">
                  <strong>{airport.name}</strong>
                  <small>{airport.city ? `${airport.city} · ${airport.country}` : airport.country}</small>
                </span>
                <Icon name="arrow" size={18} />
              </button>
            ))}
          </div>
          {suggestions.length > 0 && (
            <div className="airport-results-footer" aria-live="polite">
              <span>Showing {suggestions.length} of {total} {total === 1 ? "airport" : "airports"}</span>
              {hasMore && (
                <button
                  className="load-more-airports"
                  disabled={isLoadingMore}
                  onClick={loadMore}
                  type="button"
                >
                  {isLoadingMore ? <><Icon name="loader" size={14} /> Loading…</> : "Show more airports"}
                </button>
              )}
            </div>
          )}
        </div>
      )}
    </div>
  );
}
