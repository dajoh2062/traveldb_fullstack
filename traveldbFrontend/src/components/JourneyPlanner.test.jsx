import { useState } from "react";
import { fireEvent, render, screen } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";
import { initialBaggageProfile, initialDocumentProfile } from "../utils/journeyForm";
import JourneyPlanner from "./JourneyPlanner";

function TestPlanner() {
  const [advancedSearch, setAdvancedSearch] = useState(false);

  return (
    <JourneyPlanner
      advancedSearch={advancedSearch}
      baggage={initialBaggageProfile}
      countries={[]}
      countryError=""
      documents={{ ...initialDocumentProfile, travelerAge: "42" }}
      error=""
      fieldErrors={{}}
      isCountryLoading={false}
      isLoading={false}
      nationality=""
      nationalityQuery=""
      onAddAirport={vi.fn()}
      onAdvancedSearchChange={setAdvancedSearch}
      onBaggageChange={vi.fn()}
      onDocumentChange={vi.fn()}
      onMoveAirport={vi.fn()}
      onNationalityQueryChange={vi.fn()}
      onRemoveAirport={vi.fn()}
      onRetryCountries={vi.fn()}
      onSelectNationality={vi.fn()}
      onSubmit={event => event.preventDefault()}
      route={[]}
    />
  );
}

describe("JourneyPlanner advanced search", () => {
  it("keeps passport details hidden until the user opts in", () => {
    render(<TestPlanner />);

    const toggle = screen.getByRole("checkbox", { name: "Advanced search" });
    expect(toggle).not.toBeChecked();
    expect(screen.queryByRole("heading", { name: "Passport details" })).not.toBeInTheDocument();

    fireEvent.click(toggle);

    expect(toggle).toBeChecked();
    expect(screen.getByRole("heading", { name: "Passport details" })).toBeInTheDocument();
    expect(screen.getByRole("spinbutton", { name: "Age on departure" })).toHaveValue(42);

    fireEvent.click(toggle);
    expect(screen.queryByRole("heading", { name: "Passport details" })).not.toBeInTheDocument();

    fireEvent.click(toggle);
    expect(screen.getByRole("spinbutton", { name: "Age on departure" })).toHaveValue(42);
  });
});
