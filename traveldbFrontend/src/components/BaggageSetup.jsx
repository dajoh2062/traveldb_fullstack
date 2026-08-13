const CHECKED_BAGGAGE_OPTIONS = [
  { value: true, label: "Yes" },
  { value: false, label: "Carry-on only" },
];

const TICKET_OPTIONS = [
  { value: "SINGLE_BOOKING", label: "One booking" },
  { value: "SEPARATE_TICKETS", label: "Separate tickets" },
  { value: "UNKNOWN", label: "Not sure" },
];

const THROUGH_CHECK_OPTIONS = [
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
    <section className="planner-block baggage-setup" aria-labelledby="baggage-setup-title">
      <div className="planner-block-heading baggage-setup-heading">
        <span className="planner-step">03</span>
        <div>
          <h3 id="baggage-setup-title">Baggage</h3>
          <p>Checked-bag details</p>
        </div>
      </div>
      <div className="planner-block-content baggage-question-grid">
        <SegmentedControl
          label="Checked baggage?"
          onChange={value => onChange("checkedBaggage", value)}
          options={CHECKED_BAGGAGE_OPTIONS}
          value={baggage.checkedBaggage}
        />
        <SegmentedControl
          disabled={!baggage.checkedBaggage}
          label="Booking"
          onChange={value => onChange("ticketArrangement", value)}
          options={TICKET_OPTIONS}
          value={baggage.ticketArrangement}
        />
        <SegmentedControl
          disabled={!baggage.checkedBaggage}
          label="Bag tag destination"
          onChange={value => onChange("checkedThrough", value)}
          options={THROUGH_CHECK_OPTIONS}
          value={baggage.checkedThrough}
        />
      </div>
    </section>
  );
}
