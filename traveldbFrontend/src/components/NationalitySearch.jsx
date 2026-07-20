import { useMemo, useState } from "react";
import { normalizeSearch } from "../utils/search";
import Icon from "./Icon";

export default function NationalitySearch({
  countries,
  isLoading,
  nationality,
  query,
  onQueryChange,
  onSelect,
}) {
  const [isOpen, setIsOpen] = useState(false);

  const suggestions = useMemo(() => {
    const normalizedQuery = normalizeSearch(query);
    return countries
      .map(country => {
        const code = normalizeSearch(country.countryId);
        const name = normalizeSearch(country.countryNameEn);
        const keywords = normalizeSearch(country.keywords ?? "");
        let score = 6;
        if (code === normalizedQuery) score = 0;
        else if (code.startsWith(normalizedQuery)) score = 1;
        else if (name === normalizedQuery) score = 2;
        else if (name.startsWith(normalizedQuery)) score = 3;
        else if (name.includes(normalizedQuery)) score = 4;
        else if (keywords.includes(normalizedQuery)) score = 5;
        return { country, score };
      })
      .filter(item => !normalizedQuery || item.score < 6)
      .sort((a, b) => a.score - b.score || a.country.countryNameEn.localeCompare(b.country.countryNameEn))
      .slice(0, 8)
      .map(item => item.country);
  }, [countries, query]);

  function selectCountry(country) {
    onSelect(country);
    setIsOpen(false);
  }

  return (
    <div
      className="field nationality-field"
      onBlur={event => {
        if (!event.currentTarget.contains(event.relatedTarget)) setIsOpen(false);
      }}
    >
      <label className="field-label" htmlFor="nationality-search">Passport nationality</label>
      <span className="input-shell">
        <Icon name="globe" size={19} />
        <input
          aria-autocomplete="list"
          aria-controls="nationality-suggestions"
          aria-describedby="nationality-hint"
          aria-expanded={isOpen}
          autoComplete="off"
          id="nationality-search"
          onChange={event => {
            onQueryChange(event.target.value);
            setIsOpen(true);
          }}
          onFocus={() => setIsOpen(true)}
          onKeyDown={event => {
            if (event.key === "Enter" && suggestions.length > 0) {
              event.preventDefault();
              selectCountry(suggestions[0]);
            }
            if (event.key === "Escape") setIsOpen(false);
          }}
          placeholder="Search country or code"
          role="combobox"
          value={query}
        />
        {nationality && <span className="selected-code">{nationality}</span>}
      </span>
      <small id="nationality-hint">Search by country, nationality, or two-letter code</small>
      {isOpen && (
        <div className="dropdown" id="nationality-suggestions" role="listbox">
          {isLoading && <div className="dropdown-status"><Icon name="loader" size={16} /> Loading countries…</div>}
          {!isLoading && suggestions.length === 0 && <div className="dropdown-status">No countries found</div>}
          {suggestions.map(country => (
            <button
              className="dropdown-item country-option"
              key={country.countryId}
              onClick={() => selectCountry(country)}
              role="option"
              type="button"
            >
              <span className="airport-code">{country.countryId}</span>
              <span className="airport-meta">
                <strong>{country.countryNameEn}</strong>
                <small>Passport nationality</small>
              </span>
              {country.countryId === nationality && <Icon name="check" size={17} />}
            </button>
          ))}
        </div>
      )}
    </div>
  );
}
