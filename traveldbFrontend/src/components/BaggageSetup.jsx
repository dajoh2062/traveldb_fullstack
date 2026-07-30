import Icon from "./Icon";

const ticketOptions = [
  { value: "SINGLE_BOOKING", label: "One booking" },
  { value: "SEPARATE_TICKETS", label: "Separate tickets" },
  { value: "UNKNOWN", label: "Not sure" },
];

const throughCheckOptions = [
  { value: "YES", label: "Final destination" },
  { value: "NO", label: "Connection airport" },
  { value: "UNKNOWN", label: "Not sure yet" },
];

function SegmentedControl({ label, value, options, onChange, disabled = false }) {
  return (
    <fieldset className="baggage-question" disabled={disabled}>
      <legend>{label}</legend>
      <div className="segmented-control">
        {options.map(option => (
          <button
            aria-pressed={value === option.value}
            className={value === option.value ? "is-selected" : ""}
            key={option.value}
            onClick={() => onChange(option.value)}
            type="button"
          >
            {option.label}
          </button>
        ))}
      </div>
    </fieldset>
  );
}

export default function BaggageSetup({ baggage, onChange }) {
  return (
    <section className="baggage-setup" aria-labelledby="baggage-setup-title">
      <div className="baggage-setup-heading">
        <span className="baggage-setup-icon"><Icon name="suitcase" size={19} /></span>
        <h3 id="baggage-setup-title">Baggage</h3>
      </div>
      <div className="baggage-question-grid">
        <SegmentedControl
          label="Checked baggage?"
          onChange={value => onChange("checkedBaggage", value)}
          options={[
            { value: true, label: "Yes" },
            { value: false, label: "Carry-on only" },
          ]}
          value={baggage.checkedBaggage}
        />
        <SegmentedControl
          disabled={!baggage.checkedBaggage}
          label="Booking"
          onChange={value => onChange("ticketArrangement", value)}
          options={ticketOptions}
          value={baggage.ticketArrangement}
        />
        <SegmentedControl
          disabled={!baggage.checkedBaggage}
          label="Bag tag destination"
          onChange={value => onChange("checkedThrough", value)}
          options={throughCheckOptions}
          value={baggage.checkedThrough}
        />
      </div>
    </section>
  );
}
