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

function travelDocument(overrides = {}) {
  return {
    type: "PASSPORT",
    customType: "",
    issuingCountryCode: "NO",
    expiryDate: "2032-05-14",
    primary: false,
    ...overrides,
  };
}

const completeDocuments = {
  residenceCountryCode: "no",
  departureDate: "2030-06-01",
  travelPurpose: "tourism",
  travelerAge: "34",
  travelDocuments: [
    travelDocument({ issuingCountryCode: " no ", primary: true }),
    travelDocument({ type: "DIPLOMATIC_PASSPORT", issuingCountryCode: "SE" }),
    travelDocument({ type: "RESIDENCE_PERMIT", issuingCountryCode: "gb", expiryDate: "" }),
    travelDocument({ type: "VISA", issuingCountryCode: " us ", expiryDate: "" }),
    travelDocument({ type: "VISA", issuingCountryCode: "US", expiryDate: "" }),
  ],
};

describe("initialDocumentProfile", () => {
  it("starts with one primary passport and common traveller defaults", () => {
    expect(initialDocumentProfile).toMatchObject({
      residenceCountryCode: "",
      departureDate: "",
      travelPurpose: "TOURISM",
      travelerAge: "",
    });
    expect(initialDocumentProfile.travelDocuments).toEqual([
      expect.objectContaining({
        type: "PASSPORT",
        customType: "",
        issuingCountryCode: "",
        expiryDate: "",
        primary: true,
      }),
    ]);
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

  it("accepts multiple passports and supporting documents", () => {
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
      nationality: "Select the traveller's nationality from the search results.",
      route: "Add at least an origin and a destination.",
    });
  });

  it("returns nested errors for missing traveller and document details", () => {
    const errors = validateJourneyForm({
      nationality: "Norway",
      route: [route[0]],
      documents: {
        ...initialDocumentProfile,
        travelPurpose: "",
      },
    });

    expect(errors).toEqual({
      nationality: "Select the traveller's nationality from the search results.",
      route: "Add at least an origin and a destination.",
      residenceCountryCode: "Select your country of residence.",
      departureDate: "Enter your departure date.",
      travelPurpose: "Select the main purpose of your trip.",
      travelerAge: "Enter the traveller's age on the departure date.",
      travelDocuments: {
        0: { issuingCountryCode: "Select the issuing country." },
      },
    });
  });

  it("requires one primary document and limits the list to 20 items", () => {
    const noPrimaryErrors = validateJourneyForm({
      nationality: "NO",
      route,
      documents: {
        ...completeDocuments,
        travelDocuments: [
          travelDocument(),
          travelDocument({ issuingCountryCode: "SE" }),
        ],
      },
    });
    const tooManyErrors = validateJourneyForm({
      nationality: "NO",
      route,
      documents: {
        ...completeDocuments,
        travelDocuments: Array.from({ length: 21 }, (_, index) => (
          travelDocument({ primary: index === 0 })
        )),
      },
    });

    expect(noPrimaryErrors.travelDocuments._error).toBe(
      "Choose one primary document for this trip.",
    );
    expect(tooManyErrors.travelDocuments._error).toBe(
      "Add no more than 20 travel documents.",
    );
  });

  it("allows optional expiry and issuer fields only for the applicable types", () => {
    const documents = {
      ...completeDocuments,
      travelDocuments: [
        travelDocument({
          type: "LAISSEZ_PASSER",
          issuingCountryCode: "",
          expiryDate: "",
          primary: true,
        }),
        travelDocument({
          type: "OTHER",
          customType: "  Border crossing card  ",
          issuingCountryCode: "",
          expiryDate: "",
        }),
      ],
    };

    expect(validateJourneyForm({ nationality: "NO", route, documents })).toEqual({});
  });

  it("validates custom document names and optional expiry dates", () => {
    const documents = {
      ...completeDocuments,
      travelDocuments: [
        travelDocument({
          type: "OTHER",
          customType: "",
          expiryDate: "2030-05-31",
          primary: true,
        }),
      ],
    };

    expect(validateJourneyForm({ nationality: "NO", route, documents })).toMatchObject({
      travelDocuments: {
        0: {
          customType: "Enter a name for this document.",
          expiryDate: "This document expires before departure.",
        },
      },
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
  it("uses the tourism assumption and ignores hidden document values for a basic check", () => {
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
      documents: {
        travelPurpose: "TOURISM",
      },
    });
  });

  it("normalizes all documents and derives the legacy compatibility fields", () => {
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
        travelDocuments: [
          {
            type: "PASSPORT",
            customType: null,
            issuingCountryCode: "NO",
            expiryDate: "2032-05-14",
            primary: true,
          },
          {
            type: "DIPLOMATIC_PASSPORT",
            customType: null,
            issuingCountryCode: "SE",
            expiryDate: "2032-05-14",
            primary: false,
          },
          {
            type: "RESIDENCE_PERMIT",
            customType: null,
            issuingCountryCode: "GB",
            expiryDate: null,
            primary: false,
          },
          {
            type: "VISA",
            customType: null,
            issuingCountryCode: "US",
            expiryDate: null,
            primary: false,
          },
          {
            type: "VISA",
            customType: null,
            issuingCountryCode: "US",
            expiryDate: null,
            primary: false,
          },
        ],
      },
    });
  });

  it.each([
    "DIPLOMATIC_PASSPORT",
    "SERVICE_PASSPORT",
    "OFFICIAL_PASSPORT",
    "MILITARY_PASSPORT",
    "NATIONAL_ID_CARD",
  ])("does not populate ordinary-passport fields from a primary %s", type => {
    const documents = {
      ...completeDocuments,
      travelDocuments: [
        travelDocument({ type, primary: true }),
        travelDocument({ type: "PASSPORT", primary: false }),
      ],
    };

    expect(buildJourneyRequest({
      nationality: "NO",
      route,
      baggage: initialBaggageProfile,
      documents,
    }).documents).toMatchObject({
      passportIssuingCountryCode: null,
      passportExpiryDate: null,
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
      passportIssuingCountryCode: null,
      passportExpiryDate: null,
      departureDate: null,
      travelerAge: null,
      residencePermitCountryCodes: [],
      visaCountryCodes: [],
    });
    expect(request.documents.travelDocuments[0]).toEqual({
      type: "PASSPORT",
      customType: null,
      issuingCountryCode: null,
      expiryDate: null,
      primary: true,
    });
  });
});

describe("API error mapping", () => {
  it("maps nested and legacy API fields back to document cards", () => {
    expect(mapApiErrorsToFields([
      { field: "nationalityCountryCode", message: "Choose a nationality" },
      { field: "route[1]", message: "Unknown airport" },
      { field: "documents.travelDocuments[1].expiryDate", message: "Invalid expiry" },
      { field: "documents.passportIssuingCountryCode", message: "Unknown issuer" },
      { field: "documents.visaCountryCodes[0]", message: "Unknown visa country" },
    ], completeDocuments)).toEqual({
      nationality: "Choose a nationality",
      route: "Unknown airport",
      travelDocuments: {
        0: { issuingCountryCode: "Unknown issuer" },
        1: { expiryDate: "Invalid expiry" },
        3: { issuingCountryCode: "Unknown visa country" },
      },
    });
  });

  it("keeps journey errors when advanced document fields are hidden", () => {
    expect(removeDocumentFieldErrors({
      nationality: "Choose a nationality",
      route: "Add another airport",
      departureDate: "Choose a date",
      travelerAge: "Enter an age",
      travelDocuments: { 0: { type: "Choose a type" } },
    })).toEqual({
      nationality: "Choose a nationality",
      route: "Add another airport",
    });
  });
});
