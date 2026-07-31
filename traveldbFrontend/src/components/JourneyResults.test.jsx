import { render, screen, within } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import JourneyResults from "./JourneyResults";

const route = [
  { iataCode: "OSL", name: "Oslo Airport" },
  { iataCode: "JFK", name: "John F. Kennedy Airport" },
  { iataCode: "BNE", name: "Brisbane International Airport" },
  { iataCode: "MEL", name: "Melbourne Airport" },
];

const result = {
  baggageStops: [
    {
      airportCode: "JFK",
      status: "REQUIRED",
      title: "Collect at the first U.S. arrival",
      explanation: "Collect checked baggage for border processing before the onward flight.",
      sources: [{ label: "U.S. CBP baggage guidance", url: "https://www.cbp.gov/travel" }],
    },
    { airportCode: "BNE", status: "CONFIRM", explanation: "Verbose uncertain explanation" },
    { airportCode: "MEL", status: "NOT_REQUIRED", explanation: "No action needed" },
  ],
  documentCheck: {
    missingInputs: ["Departure date"],
    requirements: [
      {
        code: "PASSPORT",
        scope: "JOURNEY",
        status: "REQUIRED",
        title: "Valid passport",
        summary: "Verbose passport summary",
        sources: [{ label: "Passport authority", url: "https://example.gov/passports", sourceType: "GOVERNMENT" }],
      },
      { code: "PASSPORT", scope: "JOURNEY", status: "REQUIRED", title: "Valid passport", summary: "Duplicate" },
      { code: "VISA", status: "NOT_REQUIRED", title: "Visa not required" },
      {
        airportCode: "JFK",
        code: "ESTA_OR_US_VISA",
        scope: "TRANSIT",
        status: "CONDITIONAL",
        title: "Approved ESTA or valid U.S. visa",
        sources: [{ label: "U.S. government", url: "https://travel.state.gov/", sourceType: "GOVERNMENT" }],
      },
      {
        airportCode: "BNE",
        code: "AUSTRALIA_VISITOR_VISA",
        scope: "ENTRY",
        status: "CONDITIONAL",
        title: "Australian visa — eVisitor or ETA may be available",
        sources: [
          {
            label: "Australian Home Affairs — eVisitor subclass 651",
            url: "https://immi.homeaffairs.gov.au/visas/getting-a-visa/visa-listing/evisitor-651",
            sourceType: "GOVERNMENT",
          },
          {
            label: "Australian Home Affairs — Electronic Travel Authority 601",
            url: "https://immi.homeaffairs.gov.au/visas/getting-a-visa/visa-listing/electronic-travel-authority-601",
            sourceType: "GOVERNMENT",
          },
        ],
      },
      {
        airportCode: "MEL",
        code: "PASSPORT_VALIDITY",
        scope: "ENTRY",
        status: "VERIFY",
        title: "Passport validity",
      },
      { airportCode: "BNE", code: "ENTRY_CONDITIONS", scope: "ENTRY", status: "VERIFY", title: "Additional entry evidence" },
    ],
  },
};

describe("JourneyResults", () => {
  it("shows required, alternative, and unverified document actions at the correct airports", () => {
    render(<JourneyResults result={result} route={route} />);

    const baggage = screen.getByRole("region", { name: "Baggage pickup" });
    expect(within(baggage).getAllByRole("listitem")).toHaveLength(2);
    expect(within(baggage).getByText("John F. Kennedy Airport (JFK)")).toBeInTheDocument();
    expect(within(baggage).getByText("Collect at the first U.S. arrival")).toBeInTheDocument();
    expect(within(baggage).getByText("Collect checked baggage for border processing before the onward flight.")).toBeInTheDocument();
    expect(within(baggage).getByText("Pick up and recheck")).toBeInTheDocument();
    expect(within(baggage).getByRole("link", { name: "U.S. CBP baggage guidance" })).toHaveAttribute(
      "href",
      "https://www.cbp.gov/travel",
    );
    expect(within(baggage).getByText("Brisbane International Airport (BNE)")).toBeInTheDocument();
    expect(within(baggage).getByText("Confirm with airline")).toBeInTheDocument();
    expect(within(baggage).queryByText("Melbourne Airport (MEL)")).not.toBeInTheDocument();

    const documents = screen.getByRole("region", { name: "Travel documents" });
    expect(within(documents).getAllByRole("listitem")).toHaveLength(4);
    expect(within(documents).getByText("Valid passport")).toBeInTheDocument();
    expect(within(documents).getByText("Verbose passport summary")).toBeInTheDocument();
    expect(within(documents).getByText("Approved ESTA or valid U.S. visa")).toBeInTheDocument();
    expect(within(documents).getByText("John F. Kennedy Airport (JFK)")).toBeInTheDocument();
    expect(within(documents).getByText("Australian visa — eVisitor or ETA may be available")).toBeInTheDocument();
    expect(within(documents).getByText("Brisbane International Airport (BNE)")).toBeInTheDocument();
    expect(within(documents).getByRole("link", { name: "eVisitor 651" })).toHaveAttribute(
      "href",
      "https://immi.homeaffairs.gov.au/visas/getting-a-visa/visa-listing/evisitor-651",
    );
    expect(within(documents).getByRole("link", { name: "ETA 601" })).toHaveAttribute(
      "href",
      "https://immi.homeaffairs.gov.au/visas/getting-a-visa/visa-listing/electronic-travel-authority-601",
    );
    expect(within(documents).getByText("Passport validity")).toBeInTheDocument();
    expect(within(documents).getAllByText("Check")).toHaveLength(2);
    expect(within(documents).getByText("Verify")).toBeInTheDocument();
    expect(screen.queryByText("Visa not required")).not.toBeInTheDocument();
    expect(screen.queryByText("Additional entry evidence")).not.toBeInTheDocument();

    expect(screen.getByText("Use Advanced search for a more precise result.")).toBeInTheDocument();
    expect(screen.getByText(/Each recommendation links to its supporting authority or carrier guidance/)).toBeInTheDocument();
  });

  it("never presents missing coverage as no documents required", () => {
    render(<JourneyResults result={{ baggageStops: [], documentCheck: { requirements: [] } }} route={route} />);

    expect(screen.getByText("No baggage pickup identified.")).toBeInTheDocument();
    expect(screen.getByText("Requirements could not be confirmed. Check official immigration guidance.")).toBeInTheDocument();
    expect(screen.queryByText("No required documents identified.")).not.toBeInTheDocument();
  });

  it("handles missing document data and keeps a confirm-only stop", () => {
    render(<JourneyResults result={{ baggageStops: [{ airportCode: "BNE", status: "CONFIRM" }] }} route={route} />);

    expect(screen.getByText("Brisbane International Airport (BNE)")).toBeInTheDocument();
    expect(screen.getByText("Confirm with airline")).toBeInTheDocument();
    expect(screen.getByText("Requirements could not be confirmed. Check official immigration guidance.")).toBeInTheDocument();
  });

  it("keeps the same requirement code at separate itinerary stops", () => {
    render(<JourneyResults result={{
      baggageStops: [],
      documentCheck: {
        requirements: [
          { airportCode: "JFK", code: "TRANSIT_PERMISSION", scope: "TRANSIT", status: "VERIFY", title: "Transit permission" },
          { airportCode: "BNE", code: "TRANSIT_PERMISSION", scope: "TRANSIT", status: "VERIFY", title: "Transit permission" },
        ],
      },
    }} route={route} />);

    expect(screen.getAllByText("Transit permission")).toHaveLength(2);
    expect(screen.getByText("John F. Kennedy Airport (JFK)")).toBeInTheDocument();
    expect(screen.getByText("Brisbane International Airport (BNE)")).toBeInTheDocument();
  });

  it("does not present conditional transit guidance as definitely required", () => {
    render(<JourneyResults result={{
      baggageStops: [],
      documentCheck: {
        requirements: [{
          airportCode: "BNE",
          code: "AU_TRANSIT_VISA",
          scope: "TRANSIT",
          status: "CONDITIONAL",
          title: "Australian transit-without-visa conditions may apply",
        }],
      },
    }} route={route} />);

    expect(screen.getByText("Check")).toBeInTheDocument();
    expect(screen.queryByText("One required")).not.toBeInTheDocument();
  });
});
