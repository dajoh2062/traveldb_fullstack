import { render, screen, within } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import JourneyResults from "./JourneyResults";
import {
  documentRequirementKey,
  formatRuleDate,
  sortDocumentRequirementsByCountry,
} from "./resultHelpers";

const route = [
  { countryCode: "NO", iataCode: "OSL", name: "Oslo Airport" },
  { countryCode: "US", iataCode: "JFK", name: "John F. Kennedy Airport" },
  { countryCode: "AU", iataCode: "BNE", name: "Brisbane International Airport" },
  { countryCode: "AU", iataCode: "MEL", name: "Melbourne Airport" },
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
    datasetVersion: "2026-07-31.1",
    missingInputs: ["Departure date"],
    requirements: [
      {
        code: "PASSPORT",
        scope: "JOURNEY",
        status: "REQUIRED",
        title: "Valid passport",
        summary: "Verbose passport summary",
        keyFacts: [
          { label: "Passport", value: "Norwegian ordinary passport" },
          { label: "Validity", value: "Valid for the whole journey" },
        ],
        conditions: [
          "Carry the passport used for this check.",
          "The passport must be undamaged.",
          "Each child needs a separate passport.",
          "Local rule passport-validity was last verified 2026-07-30.",
        ],
        lastVerified: "2026-07-30",
        sources: [
          {
            label: "Passport authority",
            url: "https://example.gov/passports",
            sourceType: "GOVERNMENT",
          },
        ],
      },
      {
        code: "PASSPORT",
        scope: "JOURNEY",
        status: "REQUIRED",
        title: "Valid passport",
        summary: "Duplicate",
      },
      { code: "VISA", status: "NOT_REQUIRED", title: "Visa not required" },
      {
        airportCode: "JFK",
        code: "ESTA_OR_US_VISA",
        scope: "TRANSIT",
        status: "CONDITIONAL",
        title: "Approved ESTA or valid U.S. visa",
        keyFacts: [
          { label: "Visa-free stay", value: "Up to 90 days" },
          { label: "Before travel", value: "ESTA required unless you have a valid U.S. visa" },
        ],
        sources: [
          { label: "U.S. government", url: "https://travel.state.gov/", sourceType: "GOVERNMENT" },
        ],
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
      {
        airportCode: "BNE",
        code: "ENTRY_CONDITIONS",
        scope: "ENTRY",
        status: "VERIFY",
        title: "Additional entry evidence",
      },
    ],
  },
};

describe("JourneyResults", () => {
  it("rejects impossible rule dates instead of rolling them into another month", () => {
    expect(formatRuleDate("2026-02-31", "en-GB")).toBeNull();
    expect(formatRuleDate("2026-02-28", "en-GB")).toBe("28 Feb 2026");
  });

  it("keeps distinct rules separate when they share an output code", () => {
    const baseRequirement = {
      airportCode: "NRT",
      code: "JAPAN_SHORT_STAY_PERMISSION",
      scope: "ENTRY",
    };

    expect(
      documentRequirementKey({ ...baseRequirement, ruleId: "jp-entry-norwegian-passport" }),
    ).not.toBe(documentRequirementKey({ ...baseRequirement, ruleId: "jp-entry-spanish-passport" }));
  });

  it("sorts document requirements by country in itinerary order", () => {
    const sorted = sortDocumentRequirementsByCountry(
      [
        { airportCode: "BNE", code: "AU_ENTRY", title: "Australia entry" },
        { airportCode: "JFK", code: "US_TRANSIT", title: "United States transit" },
        { code: "PASSPORT", title: "Whole journey passport" },
        { airportCode: "MEL", code: "AU_PASSPORT", title: "Australia passport" },
      ],
      route,
    );

    expect(sorted.map(requirement => requirement.title)).toEqual([
      "Whole journey passport",
      "United States transit",
      "Australia entry",
      "Australia passport",
    ]);
  });

  it("shows required, alternative, and unverified document actions at the correct airports", () => {
    render(<JourneyResults result={result} route={route} />);

    const baggage = screen.getByRole("region", { name: "Baggage" });
    expect(within(baggage).getAllByRole("listitem")).toHaveLength(2);
    expect(within(baggage).getByText("John F. Kennedy Airport (JFK)")).toBeInTheDocument();
    expect(within(baggage).getByText("Collect at the first U.S. arrival")).toBeInTheDocument();
    expect(
      within(baggage).getByText(
        "Collect checked baggage for border processing before the onward flight.",
      ),
    ).toBeInTheDocument();
    expect(within(baggage).getByText("Pick up and recheck")).toBeInTheDocument();
    expect(within(baggage).queryByRole("link")).not.toBeInTheDocument();
    expect(within(baggage).getByText("Brisbane International Airport (BNE)")).toBeInTheDocument();
    expect(within(baggage).getByText("Confirm with airline")).toBeInTheDocument();
    expect(within(baggage).queryByText("Melbourne Airport (MEL)")).not.toBeInTheDocument();

    const documents = screen.getByRole("region", { name: "Travel documents" });
    expect(within(documents).getAllByRole("listitem")).toHaveLength(5);
    expect(within(documents).getByText("Valid passport")).toBeInTheDocument();
    expect(within(documents).getByText("Verbose passport summary")).toBeInTheDocument();
    expect(within(documents).getByText("Norwegian ordinary passport")).toBeInTheDocument();
    expect(within(documents).getByText("Valid for the whole journey")).toBeInTheDocument();
    expect(
      within(documents).getByText("Carry the passport used for this check."),
    ).toBeInTheDocument();
    expect(within(documents).getByText("The passport must be undamaged.")).toBeInTheDocument();
    expect(within(documents).getAllByText("View more").length).toBeGreaterThan(0);
    expect(
      within(documents).getByText("Each child needs a separate passport."),
    ).toBeInTheDocument();
    expect(within(documents).queryByText(/Local rule passport-validity/)).not.toBeInTheDocument();
    expect(within(documents).queryByText("Rule verified 30 Jul 2026")).not.toBeInTheDocument();
    expect(within(documents).getByText("Approved ESTA or valid U.S. visa")).toBeInTheDocument();
    expect(within(documents).getByText("Visa-free stay")).toBeInTheDocument();
    expect(within(documents).getByText("Up to 90 days")).toBeInTheDocument();
    expect(within(documents).getByText("Before travel")).toBeInTheDocument();
    expect(
      within(documents).getByText("ESTA required unless you have a valid U.S. visa"),
    ).toBeInTheDocument();
    expect(
      within(documents).getByText("Transit at John F. Kennedy Airport (JFK)"),
    ).toBeInTheDocument();
    expect(
      within(documents).getByText("Australian visa — eVisitor or ETA may be available"),
    ).toBeInTheDocument();
    expect(
      within(documents).getByText("Entry at Brisbane International Airport (BNE)"),
    ).toBeInTheDocument();
    expect(within(documents).queryByRole("link")).not.toBeInTheDocument();
    expect(within(documents).getByText("Passport validity")).toBeInTheDocument();
    expect(within(documents).getAllByText("Required unless exempt")).toHaveLength(2);
    expect(within(documents).getByText("Could not confirm")).toBeInTheDocument();
    expect(within(documents).getByText("Visa not required")).toBeInTheDocument();
    expect(within(documents).getByText("Not required")).toBeInTheDocument();
    expect(screen.queryByText("Additional entry evidence")).not.toBeInTheDocument();

    expect(within(documents).queryByText("Local rule set 2026-07-31.1")).not.toBeInTheDocument();
    expect(screen.getByText("Not confirmed: departure date.")).toBeInTheDocument();
    expect(screen.queryByText("6 items to review")).not.toBeInTheDocument();
    expect(screen.getByText("Recheck official guidance before travel.")).toBeInTheDocument();
  });

  it("never presents missing coverage as no documents required", () => {
    render(
      <JourneyResults
        result={{ baggageStops: [], documentCheck: { requirements: [] } }}
        route={route}
      />,
    );

    expect(screen.getByText("No baggage pickup identified.")).toBeInTheDocument();
    expect(
      screen.getByText("Requirements could not be confirmed. Check official immigration guidance."),
    ).toBeInTheDocument();
    expect(screen.queryByText("No required documents identified.")).not.toBeInTheDocument();
  });

  it("handles missing document data and keeps a confirm-only stop", () => {
    render(
      <JourneyResults
        result={{ baggageStops: [{ airportCode: "BNE", status: "CONFIRM" }] }}
        route={route}
      />,
    );

    expect(screen.getByText("Brisbane International Airport (BNE)")).toBeInTheDocument();
    expect(screen.getByText("Confirm with airline")).toBeInTheDocument();
    expect(
      screen.getByText("Requirements could not be confirmed. Check official immigration guidance."),
    ).toBeInTheDocument();
  });

  it("keeps the same requirement code at separate itinerary stops", () => {
    render(
      <JourneyResults
        result={{
          baggageStops: [],
          documentCheck: {
            requirements: [
              {
                airportCode: "JFK",
                code: "TRANSIT_PERMISSION",
                scope: "TRANSIT",
                status: "VERIFY",
                title: "Transit permission",
              },
              {
                airportCode: "BNE",
                code: "TRANSIT_PERMISSION",
                scope: "TRANSIT",
                status: "VERIFY",
                title: "Transit permission",
              },
            ],
          },
        }}
        route={route}
      />,
    );

    expect(screen.getAllByText("Transit permission")).toHaveLength(2);
    expect(screen.getByText("Transit at John F. Kennedy Airport (JFK)")).toBeInTheDocument();
    expect(screen.getByText("Transit at Brisbane International Airport (BNE)")).toBeInTheDocument();
  });

  it("does not present conditional transit guidance as definitely required", () => {
    render(
      <JourneyResults
        result={{
          baggageStops: [],
          documentCheck: {
            requirements: [
              {
                airportCode: "BNE",
                code: "AU_TRANSIT_VISA",
                scope: "TRANSIT",
                status: "CONDITIONAL",
                title: "Australian transit-without-visa conditions may apply",
              },
            ],
          },
        }}
        route={route}
      />,
    );

    expect(screen.getByText("Required unless exempt")).toBeInTheDocument();
    expect(screen.queryByText("One required")).not.toBeInTheDocument();
  });
});
