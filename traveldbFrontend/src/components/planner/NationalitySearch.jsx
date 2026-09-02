import { useMemo, useState } from "react";
import { Check, Globe2, LoaderCircle } from "lucide-react";
import { useTranslation } from "react-i18next";
import {
  buildCountrySearchIndex,
  countryDisplayName,
  searchCountryIndex,
} from "../../utils/search";
import CountryFlag from "../ui/CountryFlag";

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
  const { i18n, t } = useTranslation();
  const [isOpen, setIsOpen] = useState(false);
  const [activeIndex, setActiveIndex] = useState(0);
  const countrySearchIndex = useMemo(
    () => buildCountrySearchIndex(countries, i18n.resolvedLanguage),
    [countries, i18n.resolvedLanguage],
  );
  const suggestions = useMemo(
    () => searchCountryIndex(countrySearchIndex, query, MAX_SUGGESTIONS),
    [countrySearchIndex, query],
  );
  const resolvedActiveIndex =
    suggestions.length === 0 ? 0 : Math.min(activeIndex, suggestions.length - 1);

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
      selectCountry(suggestions[resolvedActiveIndex]);
    }
  }

  return (
    <div className={`field nationality-field ${error ? "has-error" : ""}`} onBlur={handleBlur}>
      <label className="field-label" htmlFor="nationality-search">
        {t("nationality.label")}
      </label>
      <span className="input-shell">
        {nationality ? (
          <CountryFlag countryCode={nationality} />
        ) : (
          <Globe2 aria-hidden="true" className="icon" size={19} strokeWidth={1.8} />
        )}
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
          autoCapitalize="none"
          autoComplete="off"
          enterKeyHint="search"
          id="nationality-search"
          inputMode="search"
          onChange={handleQueryChange}
          onFocus={() => setIsOpen(true)}
          onKeyDown={handleKeyDown}
          placeholder={t("nationality.placeholder")}
          role="combobox"
          spellCheck={false}
          value={query}
        />
        {nationality && <span className="selected-code">{nationality}</span>}
      </span>
      {error && (
        <span className="field-error" id="nationality-error" role="alert">
          {error}
        </span>
      )}
      {isOpen && (
        <div className="dropdown" id="nationality-suggestions" role="listbox">
          {isLoading && (
            <div className="dropdown-status">
              <LoaderCircle
                aria-hidden="true"
                className="icon is-spinning"
                size={16}
                strokeWidth={1.8}
              />
              {t("nationality.loading")}
            </div>
          )}
          {!isLoading && suggestions.length === 0 && (
            <div className="dropdown-status">{t("nationality.noResults")}</div>
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
              <span className="country-code-cell">
                <CountryFlag countryCode={country.countryId} />
                <span className="airport-code">{country.countryId}</span>
              </span>
              <span className="airport-meta">
                <strong>{countryDisplayName(country, i18n.resolvedLanguage)}</strong>
                <small>{t("nationality.optionDescription")}</small>
              </span>
              {country.countryId === nationality && (
                <Check aria-hidden="true" className="icon" size={17} strokeWidth={1.8} />
              )}
            </button>
          ))}
        </div>
      )}
    </div>
  );
}
