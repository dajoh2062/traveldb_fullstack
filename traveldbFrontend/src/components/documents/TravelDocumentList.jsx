import Icon from "../Icon";
import {
  createTravelDocument,
  getTravelDocumentType,
  MAX_TRAVEL_DOCUMENTS,
  TRAVEL_DOCUMENT_TYPES,
} from "../../utils/travelDocuments";
import CountrySelect from "./CountrySelect";
import FieldError from "./FieldError";

function TravelDocumentEditor({
  countries,
  document,
  errors = {},
  index,
  minimumExpiryDate,
  onRemove,
  onUpdate,
}) {
  const fieldPrefix = `travel-document-${index + 1}`;
  const typeDetails = getTravelDocumentType(document.type);
  const issuerLabel = typeDetails?.issuerRequired
    ? "Issuing country"
    : "Issuing country (optional)";

  return (
    <li className="travel-document-card">
      <div className="travel-document-card-header">
        <div>
          <h5>Document {index + 1}</h5>
          <small>{typeDetails?.label ?? "Travel document"}</small>
        </div>
        <button
          aria-label={`Remove document ${index + 1}`}
          className="remove-document-button"
          onClick={onRemove}
          type="button"
        >
          <Icon name="close" size={14} />
          Remove
        </button>
      </div>
      <FieldError id={`${fieldPrefix}-error`} message={errors._error} />

      <div className="travel-document-fields">
        <label className={`field document-field ${errors.type ? "has-error" : ""}`} htmlFor={`${fieldPrefix}-type`}>
          <span className="field-label">Document type</span>
          <span className="select-shell">
            <select
              aria-describedby={errors.type ? `${fieldPrefix}-type-error` : undefined}
              aria-invalid={Boolean(errors.type)}
              id={`${fieldPrefix}-type`}
              onChange={event => onUpdate({
                type: event.target.value,
                customType: event.target.value === "OTHER" ? document.customType : "",
              })}
              value={document.type}
            >
              {TRAVEL_DOCUMENT_TYPES.map(option => (
                <option key={option.value} value={option.value}>{option.label}</option>
              ))}
            </select>
          </span>
          <FieldError id={`${fieldPrefix}-type-error`} message={errors.type} />
        </label>

        {document.type === "OTHER" && (
          <label
            className={`field document-field ${errors.customType ? "has-error" : ""}`}
            htmlFor={`${fieldPrefix}-custom-type`}
          >
            <span className="field-label">Document name</span>
            <span className="input-shell document-input">
              <input
                aria-describedby={errors.customType ? `${fieldPrefix}-custom-type-error` : undefined}
                aria-invalid={Boolean(errors.customType)}
                id={`${fieldPrefix}-custom-type`}
                maxLength="80"
                onChange={event => onUpdate({ customType: event.target.value })}
                placeholder="For example, border crossing card"
                type="text"
                value={document.customType}
              />
            </span>
            <FieldError id={`${fieldPrefix}-custom-type-error`} message={errors.customType} />
          </label>
        )}

        <CountrySelect
          countries={countries}
          error={errors.issuingCountryCode}
          id={`${fieldPrefix}-issuer`}
          label={issuerLabel}
          onChange={value => onUpdate({ issuingCountryCode: value })}
          value={document.issuingCountryCode}
        />

        <label
          className={`field document-field ${errors.expiryDate ? "has-error" : ""}`}
          htmlFor={`${fieldPrefix}-expiry`}
        >
          <span className="field-label">Expiry date (optional)</span>
          <span className="input-shell document-input">
            <input
              aria-describedby={errors.expiryDate ? `${fieldPrefix}-expiry-error` : undefined}
              aria-invalid={Boolean(errors.expiryDate)}
              id={`${fieldPrefix}-expiry`}
              min={minimumExpiryDate}
              onInput={event => onUpdate({ expiryDate: event.currentTarget.value })}
              type="date"
              value={document.expiryDate}
            />
          </span>
          <FieldError id={`${fieldPrefix}-expiry-error`} message={errors.expiryDate} />
        </label>
      </div>

      <label className="primary-document-choice">
        <input
          aria-label={`Use document ${index + 1} as primary`}
          aria-describedby={errors.primary ? `${fieldPrefix}-primary-error` : undefined}
          aria-invalid={Boolean(errors.primary)}
          checked={document.primary}
          name="primary-travel-document"
          onChange={() => onUpdate({ primary: true })}
          type="radio"
        />
        <span>
          <strong>Use for this trip</strong>
          <small>The primary document is used for the journey check.</small>
        </span>
      </label>
      <FieldError id={`${fieldPrefix}-primary-error`} message={errors.primary} />
    </li>
  );
}

export default function TravelDocumentList({
  countries,
  documents = [],
  errors = {},
  minimumExpiryDate,
  onChange,
}) {
  function addDocument() {
    if (documents.length >= MAX_TRAVEL_DOCUMENTS) return;

    onChange([
      ...documents,
      createTravelDocument({ primary: documents.length === 0 }),
    ]);
  }

  function updateDocument(index, changes) {
    onChange(documents.map((document, documentIndex) => {
      if (changes.primary === true) {
        return documentIndex === index
          ? { ...document, ...changes, primary: true }
          : { ...document, primary: false };
      }
      return documentIndex === index ? { ...document, ...changes } : document;
    }));
  }

  function removeDocument(index) {
    const wasPrimary = documents[index]?.primary;
    let remainingDocuments = documents.filter((_, documentIndex) => documentIndex !== index);

    if (wasPrimary && remainingDocuments.length > 0) {
      remainingDocuments = remainingDocuments.map((document, documentIndex) => ({
        ...document,
        primary: documentIndex === 0,
      }));
    }
    onChange(remainingDocuments);
  }

  return (
    <section
      aria-describedby={errors._error ? "travel-documents-error" : undefined}
      aria-labelledby="travel-document-list-title"
      className="travel-document-list-section"
    >
      <div className="travel-document-list-header">
        <div>
          <h4 id="travel-document-list-title">Documents you will carry</h4>
          <p>Add every passport, permit, visa, or other travel document relevant to this trip.</p>
        </div>
        <button
          className="secondary-button add-document-button"
          disabled={documents.length >= MAX_TRAVEL_DOCUMENTS}
          onClick={addDocument}
          type="button"
        >
          Add document
        </button>
      </div>

      <FieldError id="travel-documents-error" message={errors._error} />

      {documents.length > 0 ? (
        <ol className="travel-document-list">
          {documents.map((document, index) => (
            <TravelDocumentEditor
              countries={countries}
              document={document}
              errors={errors[index]}
              index={index}
              key={document.clientId}
              minimumExpiryDate={minimumExpiryDate}
              onRemove={() => removeDocument(index)}
              onUpdate={changes => updateDocument(index, changes)}
            />
          ))}
        </ol>
      ) : (
        <p className="travel-document-empty">No documents added yet.</p>
      )}

      <small className="travel-document-limit">
        {documents.length} of {MAX_TRAVEL_DOCUMENTS} documents added
      </small>
    </section>
  );
}
