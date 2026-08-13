import { describe, expect, it } from "vitest";
import {
  buildJourneyRequest,
  initialBaggageProfile,
  mapApiErrorsToFields,
  validateJourneyForm,
} from "./journeyForm";

const route = [
  { iataCode: "osl", name: "Oslo Airport" },
  { iataCode: "lhr", name: "Heathrow Airport" },
];

describe("journey form", () => {
  it("uses sensible checked-baggage defaults", () => {
    expect(initialBaggageProfile).toEqual({
      checkedBaggage: true,
      ticketArrangement: "SINGLE_BOOKING",
      checkedThrough: "YES",
    });
  });

  it("requires a nationality and at least two airports", () => {
    expect(validateJourneyForm({ nationality: "Norway", route: [route[0]] })).toEqual({
      nationality: "Select the traveller's nationality from the search results.",
      route: "Add at least an origin and a destination.",
    });
  });

  it("accepts a regular-passport journey", () => {
    expect(validateJourneyForm({ nationality: "NO", route })).toEqual({});
  });

  it("builds the tourism-only API request", () => {
    expect(
      buildJourneyRequest({
        nationality: " no ",
        route: [route[0], " lhr "],
        baggage: initialBaggageProfile,
      }),
    ).toEqual({
      nationalityCountryCode: "NO",
      route: ["OSL", "LHR"],
      baggage: initialBaggageProfile,
      documents: { travelPurpose: "TOURISM" },
    });
  });

  it("maps only errors that the current form can display", () => {
    expect(
      mapApiErrorsToFields([
        { field: "nationalityCountryCode", message: "Choose a nationality" },
        { field: "route[1]", message: "Unknown airport" },
        { field: "documents.departureDate", message: "Choose a date" },
      ]),
    ).toEqual({
      nationality: "Choose a nationality",
      route: "Unknown airport",
    });
  });
});
