import { render, screen } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";
import { initialBaggageProfile } from "../utils/journeyForm";
import JourneyPlanner from "./JourneyPlanner";

function TestPlanner() {
  return (
    <JourneyPlanner
      baggage={initialBaggageProfile}
      countries={[]}
      countryError=""
      error=""
      fieldErrors={{}}
      isCountryLoading={false}
      isLoading={false}
      nationality=""
      nationalityQuery=""
      onAddAirport={vi.fn()}
      onBaggageChange={vi.fn()}
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

describe("JourneyPlanner passport search", () => {
  it("shows the ordinary-passport limitation without an advanced-search option", () => {
    render(<TestPlanner />);

    expect(screen.queryByRole("checkbox", { name: "Advanced search" })).not.toBeInTheDocument();
    expect(screen.getByRole("note")).toHaveTextContent(
      "This search is for tourism with a regular (ordinary) passport only",
    );
    expect(screen.getByRole("note")).toHaveTextContent(
      "Other passport types and travel documents are not supported",
    );
    expect(screen.queryByRole("heading", { name: "Traveller and travel documents" })).not.toBeInTheDocument();
  });
});
