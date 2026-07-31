import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import {
  buildJourneyRequest,
  initialBaggageProfile,
  initialDocumentProfile,
  mapApiErrorsToFields,
  removeDocumentFieldErrors,
  validateJourneyForm,
} from "./journeyForm";

const route = [
  { iataCode: "osl", name: "Oslo Airport" },
  { iataCode: "lhr", name: "Heathrow Airport" },
];

const completeDocuments = {
  residenceCountryCode: "no",
  passportIssuingCountryCode: " no ",
  passportExpiryDate: "2032-05-14",
  departureDate: "2030-06-01",
  travelPurpose: "tourism",
  travelerAge: "34",
  residencePermitCountryCodes: ["gb"],
  visaCountryCodes: [" us ", "US", "invalid"],
};

describe("initialDocumentProfile", () => {
  it("starts with consumer-friendly defaults", () => {
    expect(initialDocumentProfile).toEqual({
      residenceCountryCode: "",
      passportIssuingCountryCode: "",
      passportExpiryDate: "",
      departureDate: "",
      travelPurpose: "TOURISM",
      travelerAge: "",
      residencePermitCountryCodes: [],
      visaCountryCodes: [],
    });
  });
});

describe("initialBaggageProfile", () => {
  it("starts with the common checked-baggage journey selected", () => {
    expect(initialBaggageProfile).toEqual({
      checkedBaggage: true,
      ticketArrangement: "SINGLE_BOOKING",
      checkedThrough: "YES",
    });
  });
});

describe("validateJourneyForm", () => {
  beforeEach(() => {
    vi.useFakeTimers();
    vi.setSystemTime(new Date(2030, 0, 15, 12));
  });

  afterEach(() => {
    vi.useRealTimers();
  });

  it("accepts a complete traveller and journey", () => {
    expect(validateJourneyForm({
      nationality: "NO",
      route,
      documents: completeDocuments,
    })).toEqual({});
  });

  it("allows a basic check without document details", () => {
    expect(validateJourneyForm({
      nationality: "NO",
      route,
      documents: initialDocumentProfile,
      includeDocumentDetails: false,
    })).toEqual({});
  });

  it("still validates nationality and route in basic mode", () => {
    expect(validateJourneyForm({
      nationality: "Norway",
      route: [route[0]],
      includeDocumentDetails: false,
    })).toEqual({
      nationality: "Select a passport nationality from the search results.",
      route: "Add at least an origin and a destination.",
    });
  });

  it("returns field-specific errors for missing details", () => {
    const errors = validateJourneyForm({
      nationality: "Norway",
      route: [route[0]],
      documents: { ...initialDocumentProfile, travelPurpose: "" },
    });

    expect(errors).toEqual({
      nationality: "Select a passport nationality from the search results.",
      route: "Add at least an origin and a destination.",
      residenceCountryCode: "Select your country of residence.",
      passportIssuingCountryCode: "Select the country that issued your passport.",
      passportExpiryDate: "Enter your passport expiry date.",
      departureDate: "Enter your departure date.",
      travelPurpose: "Select the main purpose of your trip.",
      travelerAge: "Enter the traveller's age on the departure date.",
    });
  });

  it("rejects invalid and past travel dates", () => {
    const invalidDateErrors = validateJourneyForm({
      nationality: "NO",
      route,
      documents: { ...completeDocuments, departureDate: "2030-02-30" },
    });
    const pastDateErrors = validateJourneyForm({
      nationality: "NO",
      route,
      documents: { ...completeDocuments, departureDate: "2030-01-14" },
    });

    expect(invalidDateErrors.departureDate).toBe("Enter a valid departure date.");
    expect(pastDateErrors.departureDate).toBe("Departure date cannot be in the past.");
  });

  it("rejects a passport that expires before departure", () => {
    const errors = validateJourneyForm({
      nationality: "NO",
      route,
      documents: { ...completeDocuments, passportExpiryDate: "2030-05-31" },
    });

    expect(errors.passportExpiryDate).toBe("Your passport expires before this journey starts.");
  });

  it.each(["-1", "121", "34.5", "not-a-number"])("rejects invalid age %s", travelerAge => {
    const errors = validateJourneyForm({
      nationality: "NO",
      route,
      documents: { ...completeDocuments, travelerAge },
    });

    expect(errors.travelerAge).toBe("Enter an age from 0 to 120.");
  });
});

describe("buildJourneyRequest", () => {
  it("ignores hidden document values for a basic check", () => {
    expect(buildJourneyRequest({
      nationality: "NO",
      route,
      baggage: initialBaggageProfile,
      documents: completeDocuments,
      includeDocumentDetails: false,
    })).toEqual({
      nationalityCountryCode: "NO",
      route: ["OSL", "LHR"],
      baggage: initialBaggageProfile,
      documents: null,
    });
  });

  it("normalizes the API payload without mutating the baggage choices", () => {
    const baggage = {
      checkedBaggage: true,
      ticketArrangement: "SINGLE_BOOKING",
      checkedThrough: "YES",
    };

    expect(buildJourneyRequest({
      nationality: " no ",
      route,
      baggage,
      documents: completeDocuments,
    })).toEqual({
      nationalityCountryCode: "NO",
      route: ["OSL", "LHR"],
      baggage,
      documents: {
        residenceCountryCode: "NO",
        passportIssuingCountryCode: "NO",
        passportExpiryDate: "2032-05-14",
        departureDate: "2030-06-01",
        travelPurpose: "TOURISM",
        travelerAge: 34,
        residencePermitCountryCodes: ["GB"],
        visaCountryCodes: ["US"],
      },
    });
  });

  it("uses nulls and empty lists for optional incomplete document values", () => {
    const request = buildJourneyRequest({
      nationality: "NO",
      route: ["OSL", "LHR"],
      baggage: { checkedBaggage: false },
      documents: initialDocumentProfile,
    });

    expect(request.documents).toMatchObject({
      passportExpiryDate: null,
      departureDate: null,
      travelerAge: null,
      residencePermitCountryCodes: [],
      visaCountryCodes: [],
    });
  });
});

describe("API error mapping", () => {
  it("maps nested API fields back to the form fields", () => {
    expect(mapApiErrorsToFields([
      { field: "nationalityCountryCode", message: "Choose a nationality" },
      { field: "route[1]", message: "Unknown airport" },
      { field: "documents.visaCountryCodes[0]", message: "Unknown country" },
    ])).toEqual({
      nationality: "Choose a nationality",
      route: "Unknown airport",
      visaCountryCodes: "Unknown country",
    });
  });

  it("keeps journey errors when advanced document fields are hidden", () => {
    expect(removeDocumentFieldErrors({
      nationality: "Choose a nationality",
      route: "Add another airport",
      departureDate: "Choose a date",
      travelerAge: "Enter an age",
    })).toEqual({
      nationality: "Choose a nationality",
      route: "Add another airport",
    });
  });
});
