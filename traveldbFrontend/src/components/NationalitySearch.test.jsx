import { fireEvent, render, screen } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";
import NationalitySearch from "./NationalitySearch";

const countries = [
  { countryId: "SE", countryNameEn: "Sweden" },
  { countryId: "NO", countryNameEn: "Norway" },
  { countryId: "DK", countryNameEn: "Denmark" },
];

describe("NationalitySearch", () => {
  it("keeps keyboard navigation aligned with alphabetized suggestions", () => {
    const onSelect = vi.fn();
    render(
      <NationalitySearch
        countries={countries}
        error=""
        isLoading={false}
        nationality=""
        query=""
        onQueryChange={vi.fn()}
        onSelect={onSelect}
      />,
    );

    const input = screen.getByRole("combobox", { name: "Traveller nationality" });
    fireEvent.focus(input);
    fireEvent.keyDown(input, { key: "ArrowDown" });
    fireEvent.keyDown(input, { key: "Enter" });

    expect(onSelect).toHaveBeenCalledWith(countries[1]);
  });
});
