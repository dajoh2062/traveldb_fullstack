import FieldError from "./FieldError";

export default function CountrySelect({ countries, error, id, label, onChange, value }) {
  const errorId = `${id}-error`;
  const emptyOption = countries.length === 0 ? "Countries unavailable" : "Select a country";

  return (
    <label className={`field document-field ${error ? "has-error" : ""}`} htmlFor={id}>
      <span className="field-label">{label}</span>
      <span className="select-shell">
        <select
          aria-describedby={error ? errorId : undefined}
          aria-invalid={Boolean(error)}
          disabled={countries.length === 0}
          id={id}
          onChange={event => onChange(event.target.value)}
          value={value}
        >
          <option value="">{emptyOption}</option>
          {countries.map(country => (
            <option key={country.countryId} value={country.countryId}>
              {country.countryNameEn}
            </option>
          ))}
        </select>
      </span>
      <FieldError id={errorId} message={error} />
    </label>
  );
}
