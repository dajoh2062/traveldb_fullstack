import { useMemo, useState } from "react";
import { buildCountrySearchIndex, searchCountryIndex } from "../utils/search";
import Icon from "./Icon";

const MAX_SUGGESTIONS = 8;

export default function NationalitySearch({
  countries,
  error,
  isLoading,
  nationality,
  query,
  onQueryChange,
  onSelect,
}) {
  const [isOpen, setIsOpen] = useState(false);
  const [activeIndex, setActiveIndex] = useState(0);
  const countrySearchIndex = useMemo(
    () => buildCountrySearchIndex(countries),
    [countries],
  );
  const suggestions = useMemo(
    () => searchCountryIndex(countrySearchIndex, query, MAX_SUGGESTIONS),
    [countrySearchIndex, query],
  );
  const resolvedActiveIndex = suggestions.length === 0
    ? 0
    : Math.min(activeIndex, suggestions.length - 1);

  function selectCountry(country) {
    onSelect(country);
    setIsOpen(false);
  }

  function handleBlur(event) {
    if (!event.currentTarget.contains(event.relatedTarget)) setIsOpen(false);
  }

  function handleQueryChange(event) {
    onQueryChange(event.target.value);
    setIsOpen(true);
    setActiveIndex(0);
  }

  function handleKeyDown(event) {
    if (event.key === "Escape") {
      setIsOpen(false);
      return;
    }
    if (suggestions.length === 0) return;

    if (event.key === "ArrowDown") {
      event.preventDefault();
      setActiveIndex(index => Math.min(index + 1, suggestions.length - 1));
    } else if (event.key === "ArrowUp") {
      event.preventDefault();
      setActiveIndex(index => Math.max(index - 1, 0));
    } else if (event.key === "Enter") {
      event.preventDefault();
      selectCountry(suggestions[resolvedActiveIndex]);
    }
  }

  return (
    <div className={`field nationality-field ${error ? "has-error" : ""}`} onBlur={handleBlur}>
      <label className="field-label" htmlFor="nationality-search">Traveller nationality</label>
      <span className="input-shell">
        <Icon name="globe" size={19} />
        <input
          aria-autocomplete="list"
          aria-controls="nationality-suggestions"
          aria-describedby={error ? "nationality-error" : undefined}
          aria-activedescendant={
            isOpen && suggestions[resolvedActiveIndex]
              ? `nationality-option-${suggestions[resolvedActiveIndex].countryId}`
              : undefined
          }
          aria-expanded={isOpen}
          aria-invalid={Boolean(error)}
          autoComplete="off"
          id="nationality-search"
          onChange={handleQueryChange}
          onFocus={() => setIsOpen(true)}
          onKeyDown={handleKeyDown}
          placeholder="Search country or code"
          role="combobox"
          value={query}
        />
        {nationality && <span className="selected-code">{nationality}</span>}
      </span>
      {error && <span className="field-error" id="nationality-error" role="alert">{error}</span>}
      {isOpen && (
        <div className="dropdown" id="nationality-suggestions" role="listbox">
          {isLoading && (
            <div className="dropdown-status"><Icon name="loader" size={16} /> Loading countries…</div>
          )}
          {!isLoading && suggestions.length === 0 && (
            <div className="dropdown-status">No countries found</div>
          )}
          {suggestions.map((country, index) => (
            <button
              aria-selected={index === resolvedActiveIndex}
              className={`dropdown-item country-option ${index === resolvedActiveIndex ? "is-active" : ""}`}
              id={`nationality-option-${country.countryId}`}
              key={country.countryId}
              onMouseMove={() => setActiveIndex(index)}
              onClick={() => selectCountry(country)}
              role="option"
              type="button"
            >
              <span className="airport-code">{country.countryId}</span>
              <span className="airport-meta">
                <strong>{country.countryNameEn}</strong>
                <small>Traveller nationality</small>
              </span>
              {country.countryId === nationality && <Icon name="check" size={17} />}
            </button>
          ))}
        </div>
      )}
    </div>
  );
}
