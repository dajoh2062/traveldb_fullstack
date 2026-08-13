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

const DEFAULT_MESSAGES = {
  nationality: "Select the traveller's nationality from the search results.",
  route: "Add at least an origin and a destination.",
};

function translate(translationFunction, key, defaultValue) {
  return translationFunction ? translationFunction(key, { defaultValue }) : defaultValue;
}

export function validateJourneyForm({ nationality, route } = {}, translationFunction) {
  const errors = {};

  if (!COUNTRY_CODE_PATTERN.test(nationality?.trim?.() ?? "")) {
    errors.nationality = translate(
      translationFunction,
      "validation.nationalityRequired",
      DEFAULT_MESSAGES.nationality,
    );
  }

  if (normalizeRoute(route).length < 2) {
    errors.route = translate(
      translationFunction,
      "validation.routeMinimum",
      DEFAULT_MESSAGES.route,
    );
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

export function mapApiErrorsToFields(errors = [], translationFunction) {
  const fieldErrors = {};

  for (const error of errors) {
    if (error.field === "nationalityCountryCode") {
      fieldErrors.nationality = translate(
        translationFunction,
        "validation.nationalityInvalid",
        error.message,
      );
    } else if (error.field === "route" || error.field?.startsWith("route[")) {
      fieldErrors.route = translate(translationFunction, "validation.routeInvalid", error.message);
    }
  }

  return fieldErrors;
}
