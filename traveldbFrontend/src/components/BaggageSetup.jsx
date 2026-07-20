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

function SegmentedControl({ label, hint, value, options, onChange, disabled = false }) {
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
      <small>{hint}</small>
    </fieldset>
  );
}

export default function BaggageSetup({ baggage, onChange }) {
  return (
    <section className="baggage-setup" aria-labelledby="baggage-setup-title">
      <div className="baggage-setup-heading">
        <span className="baggage-setup-icon"><Icon name="suitcase" size={19} /></span>
        <div>
          <h3 id="baggage-setup-title">Baggage details</h3>
          <p>These details prevent the checker from assuming every airline transfers bags in the same way.</p>
        </div>
      </div>
      <div className="baggage-question-grid">
        <SegmentedControl
          hint="Carry-on bags never need baggage reclaim."
          label="Travelling with checked baggage?"
          onChange={value => onChange("checkedBaggage", value)}
          options={[
            { value: true, label: "Yes" },
            { value: false, label: "Carry-on only" },
          ]}
          value={baggage.checkedBaggage}
        />
        <SegmentedControl
          disabled={!baggage.checkedBaggage}
          hint="Separate bookings normally require a self-transfer."
          label="How are the flights booked?"
          onChange={value => onChange("ticketArrangement", value)}
          options={ticketOptions}
          value={baggage.ticketArrangement}
        />
        <SegmentedControl
          disabled={!baggage.checkedBaggage}
          hint="Use the destination printed on your baggage tag, if known."
          label="Where will the bag be tagged to?"
          onChange={value => onChange("checkedThrough", value)}
          options={throughCheckOptions}
          value={baggage.checkedThrough}
        />
      </div>
    </section>
  );
}
