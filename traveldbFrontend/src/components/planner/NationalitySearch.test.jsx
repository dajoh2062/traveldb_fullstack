import { fireEvent, render, screen } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";
import NationalitySearch from "./NationalitySearch";

const countries = [
  { countryId: "SE", countryNameEn: "Sweden" },
  { countryId: "NO", countryNameEn: "Norway" },
  { countryId: "DK", countryNameEn: "Denmark" },
];

describe("NationalitySearch", () => {
  it("does not submit the journey when the search has no selectable result", () => {
    render(
      <NationalitySearch
        countries={[]}
        error=""
        isLoading={false}
        nationality=""
        query=""
        onQueryChange={vi.fn()}
        onSelect={vi.fn()}
      />,
    );

    const input = screen.getByRole("combobox", { name: "Traveller nationality" });

    expect(fireEvent.keyDown(input, { key: "Enter" })).toBe(false);
  });

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

  it("shows country flags in the suggestions and selected nationality", () => {
    const { rerender } = render(
      <NationalitySearch
        countries={countries}
        error=""
        isLoading={false}
        nationality=""
        query=""
        onQueryChange={vi.fn()}
        onSelect={vi.fn()}
      />,
    );

    fireEvent.focus(screen.getByRole("combobox", { name: "Traveller nationality" }));
    expect(document.querySelectorAll(".country-flag")).toHaveLength(3);

    rerender(
      <NationalitySearch
        countries={countries}
        error=""
        isLoading={false}
        nationality="NO"
        query="Norway"
        onQueryChange={vi.fn()}
        onSelect={vi.fn()}
      />,
    );
    expect(document.querySelector(".input-shell > .country-flag")).toHaveAttribute(
      "data-country-code",
      "no",
    );
  });
});
