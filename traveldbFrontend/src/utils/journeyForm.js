const COUNTRY_CODE_PATTERN = /^[A-Za-z]{2}$/;

export const initialBaggageProfile = {
  checkedBaggage: true,
  ticketArrangement: "SINGLE_BOOKING",
  checkedThrough: "YES",
};

function normalizeCountryCode(value) {
  return typeof value === "string" ? value.trim().toUpperCase() : "";
}

function normalizeRoute(route) {
  if (!Array.isArray(route)) return [];

  return route
    .map(stop => (typeof stop === "string" ? stop : stop?.iataCode))
    .filter(code => typeof code === "string" && code.trim())
    .map(code => code.trim().toUpperCase());
}

export function validateJourneyForm({ nationality, route } = {}) {
  const errors = {};

  if (!COUNTRY_CODE_PATTERN.test(nationality?.trim?.() ?? "")) {
    errors.nationality = "Select the traveller's nationality from the search results.";
  }

  if (normalizeRoute(route).length < 2) {
    errors.route = "Add at least an origin and a destination.";
  }

  return errors;
}

export function buildJourneyRequest({ nationality, route, baggage } = {}) {
  return {
    nationalityCountryCode: normalizeCountryCode(nationality),
    route: normalizeRoute(route),
    baggage,
    documents: { travelPurpose: "TOURISM" },
  };
}

export function mapApiErrorsToFields(errors = []) {
  const fieldErrors = {};

  for (const error of errors) {
    if (error.field === "nationalityCountryCode") {
      fieldErrors.nationality = error.message;
    } else if (error.field === "route" || error.field?.startsWith("route[")) {
      fieldErrors.route = error.message;
    }
  }

  return fieldErrors;
}
