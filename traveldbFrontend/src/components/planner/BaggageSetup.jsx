import { useTranslation } from "react-i18next";

const CHECKED_BAGGAGE_OPTIONS = [
  { value: true, labelKey: "baggage.form.options.yes" },
  { value: false, labelKey: "baggage.form.options.carryOnOnly" },
];

const TICKET_OPTIONS = [
  { value: "SINGLE_BOOKING", labelKey: "baggage.form.options.oneBooking" },
  { value: "SEPARATE_TICKETS", labelKey: "baggage.form.options.separateTickets" },
  { value: "UNKNOWN", labelKey: "baggage.form.options.notSure" },
];

const THROUGH_CHECK_OPTIONS = [
  { value: "YES", labelKey: "baggage.form.options.finalDestination" },
  { value: "NO", labelKey: "baggage.form.options.connectionAirport" },
  { value: "UNKNOWN", labelKey: "baggage.form.options.notSureYet" },
];

function SegmentedControl({ label, value, options, onChange, disabled = false }) {
  const { t } = useTranslation();

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
            {t(option.labelKey)}
          </button>
        ))}
      </div>
    </fieldset>
  );
}

export default function BaggageSetup({ baggage, onChange }) {
  const { t } = useTranslation();

  return (
    <section className="planner-block baggage-setup" aria-labelledby="baggage-setup-title">
      <div className="planner-block-heading baggage-setup-heading">
        <span className="planner-step">03</span>
        <div>
          <h3 id="baggage-setup-title">{t("baggage.form.title")}</h3>
          <p>{t("baggage.form.description")}</p>
        </div>
      </div>
      <div className="planner-block-content baggage-question-grid">
        <SegmentedControl
          label={t("baggage.form.checkedQuestion")}
          onChange={value => onChange("checkedBaggage", value)}
          options={CHECKED_BAGGAGE_OPTIONS}
          value={baggage.checkedBaggage}
        />
        <SegmentedControl
          disabled={!baggage.checkedBaggage}
          label={t("baggage.form.bookingQuestion")}
          onChange={value => onChange("ticketArrangement", value)}
          options={TICKET_OPTIONS}
          value={baggage.ticketArrangement}
        />
        <SegmentedControl
          disabled={!baggage.checkedBaggage}
          label={t("baggage.form.tagQuestion")}
          onChange={value => onChange("checkedThrough", value)}
          options={THROUGH_CHECK_OPTIONS}
          value={baggage.checkedThrough}
        />
      </div>
    </section>
  );
}
