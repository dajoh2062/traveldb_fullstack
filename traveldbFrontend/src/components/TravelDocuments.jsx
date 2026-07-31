import Icon from "./Icon";
import CountrySelect from "./documents/CountrySelect";
import FieldError from "./documents/FieldError";
import TravelDocumentList from "./documents/TravelDocumentList";

const TRAVEL_PURPOSE_OPTIONS = [
  { value: "TOURISM", label: "Holiday or visiting" },
  { value: "BUSINESS", label: "Business" },
  { value: "TRANSIT", label: "Transit only" },
  { value: "STUDY", label: "Study" },
  { value: "WORK", label: "Work" },
  { value: "OTHER", label: "Another purpose" },
];

function localDateString(date = new Date()) {
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, "0");
  const day = String(date.getDate()).padStart(2, "0");
  return `${year}-${month}-${day}`;
}

export default function TravelDocuments({ countries, documents, errors = {}, onChange }) {
  const today = localDateString();

  return (
    <section className="document-setup" aria-labelledby="document-setup-title">
      <div className="setup-heading">
        <span className="setup-icon"><Icon name="document" size={19} /></span>
        <div className="setup-heading-copy">
          <h3 id="document-setup-title">Traveller and travel documents</h3>
          <p>Tell us which documents you plan to carry. Do not enter document numbers.</p>
        </div>
      </div>

      <div className="document-form-grid">
        <CountrySelect
          countries={countries}
          error={errors.residenceCountryCode}
          id="residence-country"
          label="Country of residence"
          onChange={value => onChange("residenceCountryCode", value)}
          value={documents.residenceCountryCode}
        />

        <label className={`field document-field ${errors.departureDate ? "has-error" : ""}`} htmlFor="departure-date">
          <span className="field-label">Departure date</span>
          <span className="input-shell document-input">
            <input
              aria-describedby={errors.departureDate ? "departure-date-error" : undefined}
              aria-invalid={Boolean(errors.departureDate)}
              id="departure-date"
              min={today}
              onInput={event => onChange("departureDate", event.currentTarget.value)}
              type="date"
              value={documents.departureDate}
            />
          </span>
          <FieldError id="departure-date-error" message={errors.departureDate} />
        </label>

        <label className={`field document-field ${errors.travelPurpose ? "has-error" : ""}`} htmlFor="travel-purpose">
          <span className="field-label">Travel purpose</span>
          <span className="select-shell">
            <select
              aria-describedby={errors.travelPurpose ? "travel-purpose-error" : undefined}
              aria-invalid={Boolean(errors.travelPurpose)}
              id="travel-purpose"
              onChange={event => onChange("travelPurpose", event.target.value)}
              value={documents.travelPurpose}
            >
              {TRAVEL_PURPOSE_OPTIONS.map(purpose => (
                <option key={purpose.value} value={purpose.value}>{purpose.label}</option>
              ))}
            </select>
          </span>
          <FieldError id="travel-purpose-error" message={errors.travelPurpose} />
        </label>

        <label className={`field document-field ${errors.travelerAge ? "has-error" : ""}`} htmlFor="traveller-age">
          <span className="field-label">Age on departure</span>
          <span className="input-shell document-input">
            <input
              aria-describedby={errors.travelerAge ? "traveller-age-error" : undefined}
              aria-invalid={Boolean(errors.travelerAge)}
              id="traveller-age"
              inputMode="numeric"
              max="120"
              min="0"
              onChange={event => onChange("travelerAge", event.target.value)}
              placeholder="For example, 30"
              type="number"
              value={documents.travelerAge}
            />
          </span>
          <FieldError id="traveller-age-error" message={errors.travelerAge} />
        </label>
      </div>

      <TravelDocumentList
        countries={countries}
        documents={documents.travelDocuments}
        errors={errors.travelDocuments}
        minimumExpiryDate={documents.departureDate || today}
        onChange={value => onChange("travelDocuments", value)}
      />
    </section>
  );
}
