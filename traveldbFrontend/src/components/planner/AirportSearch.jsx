import { useRef, useState } from "react";
import { ArrowRight, LoaderCircle, Search } from "lucide-react";
import { useTranslation } from "react-i18next";
import useAirportSearch from "../../hooks/useAirportSearch";
import { countryDisplayName } from "../../utils/search";
import CountryFlag from "../ui/CountryFlag";

const MAX_SEARCH_LENGTH = 100;

export default function AirportSearch({ onSelect }) {
  const { i18n, t } = useTranslation();
  const inputRef = useRef(null);
  const [activeIndex, setActiveIndex] = useState(0);
  const {
    clearSearch,
    hasMore,
    isLoadingMore,
    isOpen,
    isSearching,
    loadMore,
    query,
    searchError,
    setIsOpen,
    suggestions,
    updateQuery,
  } = useAirportSearch();

  const resolvedActiveIndex =
    suggestions.length === 0 ? 0 : Math.min(activeIndex, suggestions.length - 1);

  function localizedAirportLocation(airport) {
    const countryName = countryDisplayName(
      { countryId: airport.countryCode, countryNameEn: airport.country },
      i18n.resolvedLanguage,
    );
    return airport.city ? `${airport.city} · ${countryName}` : countryName;
  }

  function selectAirport(airport) {
    if (onSelect(airport) !== false) {
      clearSearch();
      inputRef.current?.blur();
    }
  }

  function handleBlur(event) {
    if (!event.currentTarget.contains(event.relatedTarget)) setIsOpen(false);
  }

  function handleQueryChange(event) {
    updateQuery(event.target.value);
    setActiveIndex(0);
  }

  function handleKeyDown(event) {
    if (event.key === "Escape") {
      setIsOpen(false);
      return;
    }
    if (suggestions.length === 0) {
      if (event.key === "Enter") event.preventDefault();
      return;
    }

    if (event.key === "ArrowDown") {
      event.preventDefault();
      setActiveIndex(index => Math.min(index + 1, suggestions.length - 1));
    } else if (event.key === "ArrowUp") {
      event.preventDefault();
      setActiveIndex(index => Math.max(index - 1, 0));
    } else if (event.key === "Enter") {
      event.preventDefault();
      selectAirport(suggestions[resolvedActiveIndex]);
    }
  }

  return (
    <div className="field airport-field" onBlur={handleBlur}>
      <label className="field-label" htmlFor="airport-search">
        {t("airport.label")}
      </label>
      <span className="input-shell">
        <Search aria-hidden="true" className="icon" size={20} strokeWidth={1.8} />
        <input
          aria-autocomplete="list"
          aria-activedescendant={
            isOpen && suggestions[resolvedActiveIndex]
              ? `airport-option-${suggestions[resolvedActiveIndex].iataCode}`
              : undefined
          }
          aria-controls="airport-suggestions"
          aria-expanded={isOpen}
          autoCapitalize="none"
          autoComplete="off"
          enterKeyHint="search"
          id="airport-search"
          inputMode="search"
          maxLength={MAX_SEARCH_LENGTH}
          onChange={handleQueryChange}
          onFocus={() => {
            if (query.trim()) setIsOpen(true);
          }}
          onKeyDown={handleKeyDown}
          placeholder={t("airport.placeholder")}
          ref={inputRef}
          role="combobox"
          spellCheck={false}
          value={query}
        />
        {isSearching && (
          <LoaderCircle
            aria-hidden="true"
            className="icon is-spinning"
            size={17}
            strokeWidth={1.8}
          />
        )}
      </span>
      {isOpen && query.trim() && (
        <div className="dropdown airport-dropdown">
          <div className="airport-dropdown-results" id="airport-suggestions" role="listbox">
            {isSearching && suggestions.length === 0 && (
              <div className="dropdown-status">
                <LoaderCircle
                  aria-hidden="true"
                  className="icon is-spinning"
                  size={16}
                  strokeWidth={1.8}
                />
                {t("airport.searching")}
              </div>
            )}
            {!isSearching && suggestions.length === 0 && (
              <div className={`dropdown-status ${searchError ? "search-error" : ""}`}>
                {searchError || t("airport.noResults")}
              </div>
            )}
            {suggestions.map((airport, index) => (
              <button
                aria-selected={index === resolvedActiveIndex}
                className={`dropdown-item ${index === resolvedActiveIndex ? "is-active" : ""}`}
                id={`airport-option-${airport.iataCode}`}
                key={airport.iataCode}
                onMouseDown={event => event.preventDefault()}
                onMouseMove={() => setActiveIndex(index)}
                onClick={() => selectAirport(airport)}
                role="option"
                type="button"
              >
                <span className="country-code-cell">
                  <CountryFlag countryCode={airport.countryCode} />
                  <span className="airport-code">{airport.iataCode}</span>
                </span>
                <span className="airport-meta">
                  <strong dir="auto">{airport.name}</strong>
                  <small dir="auto">{localizedAirportLocation(airport)}</small>
                </span>
                <ArrowRight aria-hidden="true" className="icon" size={18} strokeWidth={1.8} />
              </button>
            ))}
          </div>
          {suggestions.length > 0 && hasMore && (
            <div className="airport-results-footer" aria-live="polite">
              <button
                className="load-more-airports"
                disabled={isLoadingMore}
                onClick={loadMore}
                type="button"
              >
                {isLoadingMore ? (
                  <>
                    <LoaderCircle
                      aria-hidden="true"
                      className="icon is-spinning"
                      size={14}
                      strokeWidth={1.8}
                    />
                    {t("airport.loadingMore")}
                  </>
                ) : (
                  t("airport.showMore")
                )}
              </button>
            </div>
          )}
          {searchError && suggestions.length > 0 && (
            <div className="airport-search-warning">{searchError}</div>
          )}
        </div>
      )}
    </div>
  );
}
