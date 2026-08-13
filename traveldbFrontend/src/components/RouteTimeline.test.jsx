import { render } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";
import RouteTimeline from "./RouteTimeline";

describe("RouteTimeline", () => {
  it("shows the country flag for each airport stop", () => {
    const { container } = render(
      <RouteTimeline
        error=""
        onMove={vi.fn()}
        onRemove={vi.fn()}
        route={[
          {
            city: "Oslo",
            country: "Norway",
            countryCode: "NO",
            iataCode: "OSL",
            name: "Oslo Airport",
          },
          {
            city: "London",
            country: "United Kingdom",
            countryCode: "GB",
            iataCode: "LHR",
            name: "London Heathrow Airport",
          },
        ]}
      />,
    );

    expect(container.querySelectorAll(".route-stop .country-flag")).toHaveLength(2);
    expect(container.querySelector('[data-country-code="no"]')).toBeInTheDocument();
    expect(container.querySelector('[data-country-code="gb"]')).toBeInTheDocument();
  });
});
