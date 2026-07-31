import { useMemo, useState } from "react";
import Icon from "../Icon";

export default function HeldCountryList({ countries, label, onChange, values }) {
  const [selection, setSelection] = useState("");
  const availableCountries = useMemo(
    () => countries.filter(country => !values.includes(country.countryId)),
    [countries, values],
  );

  function addCountry() {
    if (!selection || values.includes(selection)) return;

    onChange([...values, selection]);
    setSelection("");
  }

  return (
    <div className="held-document-list">
      <span className="field-label">{label}</span>
      <div className="held-document-picker">
        <span className="select-shell compact">
          <select
            aria-label={`Add a country to ${label.toLowerCase()}`}
            onChange={event => setSelection(event.target.value)}
            value={selection}
          >
            <option value="">Select a country</option>
            {availableCountries.map(country => (
              <option key={country.countryId} value={country.countryId}>
                {country.countryNameEn}
              </option>
            ))}
          </select>
        </span>
        <button className="secondary-button" disabled={!selection} onClick={addCountry} type="button">
          Add
        </button>
      </div>

      {values.length > 0 ? (
        <ul className="country-chip-list" aria-label={label}>
          {values.map(code => {
            const countryName = countries.find(country => country.countryId === code)?.countryNameEn ?? code;
            return (
              <li key={code}>
                <span>{countryName}</span>
                <button
                  aria-label={`Remove ${countryName}`}
                  onClick={() => onChange(values.filter(value => value !== code))}
                  type="button"
                >
                  <Icon name="close" size={13} />
                </button>
              </li>
            );
          })}
        </ul>
      ) : (
        <small>None added</small>
      )}
    </div>
  );
}
