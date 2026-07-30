export const initialDocumentProfile = {
  residenceCountryCode: "",
  passportIssuingCountryCode: "",
  passportExpiryDate: "",
  departureDate: "",
  travelPurpose: "TOURISM",
  travelerAge: "",
  residencePermitCountryCodes: [],
  visaCountryCodes: [],
};

export const initialBaggageProfile = {
  checkedBaggage: true,
  ticketArrangement: "SINGLE_BOOKING",
  checkedThrough: "YES",
};

const countryCodePattern = /^[A-Za-z]{2}$/;
const isoDatePattern = /^\d{4}-\d{2}-\d{2}$/;

function isCountryCode(value) {
  return typeof value === "string" && countryCodePattern.test(value.trim());
}

function isIsoDate(value) {
  if (typeof value !== "string" || !isoDatePattern.test(value)) return false;

  const [year, month, day] = value.split("-").map(Number);
  const date = new Date(Date.UTC(year, month - 1, day));
  return date.getUTCFullYear() === year
    && date.getUTCMonth() === month - 1
    && date.getUTCDate() === day;
}

function localToday() {
  const today = new Date();
  const year = today.getFullYear();
  const month = String(today.getMonth() + 1).padStart(2, "0");
  const day = String(today.getDate()).padStart(2, "0");
  return `${year}-${month}-${day}`;
}

function normalizeCountryCode(value) {
  return typeof value === "string" ? value.trim().toUpperCase() : "";
}

function normalizeCountryCodeList(values) {
  if (!Array.isArray(values)) return [];

  return [...new Set(values
    .map(normalizeCountryCode)
    .filter(code => countryCodePattern.test(code)))];
}

function normalizeRoute(route) {
  if (!Array.isArray(route)) return [];

  return route
    .map(stop => typeof stop === "string" ? stop : stop?.iataCode)
    .filter(code => typeof code === "string" && code.trim())
    .map(code => code.trim().toUpperCase());
}

export function validateJourneyForm({ nationality, route, documents, includeDocumentDetails = true } = {}) {
  const errors = {};

  if (!isCountryCode(nationality)) {
    errors.nationality = "Select a passport nationality from the search results.";
  }

  if (normalizeRoute(route).length < 2) {
    errors.route = "Add at least an origin and a destination.";
  }

  if (!includeDocumentDetails) return errors;

  const profile = documents ?? {};
  const departureDate = profile.departureDate?.trim?.() ?? "";
  const passportExpiryDate = profile.passportExpiryDate?.trim?.() ?? "";
  const travelPurpose = profile.travelPurpose?.trim?.() ?? "";
  const travelerAge = typeof profile.travelerAge === "string"
    ? profile.travelerAge.trim()
    : profile.travelerAge;

  if (!isCountryCode(profile.residenceCountryCode)) {
    errors.residenceCountryCode = "Select your country of residence.";
  }

  if (!isCountryCode(profile.passportIssuingCountryCode)) {
    errors.passportIssuingCountryCode = "Select the country that issued your passport.";
  }

  if (!passportExpiryDate) {
    errors.passportExpiryDate = "Enter your passport expiry date.";
  } else if (!isIsoDate(passportExpiryDate)) {
    errors.passportExpiryDate = "Enter a valid passport expiry date.";
  }

  if (!departureDate) {
    errors.departureDate = "Enter your departure date.";
  } else if (!isIsoDate(departureDate)) {
    errors.departureDate = "Enter a valid departure date.";
  } else if (departureDate < localToday()) {
    errors.departureDate = "Departure date cannot be in the past.";
  }

  if (
    isIsoDate(passportExpiryDate)
    && isIsoDate(departureDate)
    && passportExpiryDate < departureDate
  ) {
    errors.passportExpiryDate = "Your passport expires before this journey starts.";
  }

  if (!travelPurpose) {
    errors.travelPurpose = "Select the main purpose of your trip.";
  }

  if (travelerAge === "" || travelerAge === null || travelerAge === undefined) {
    errors.travelerAge = "Enter the traveller's age on the departure date.";
  } else {
    const numericAge = Number(travelerAge);
    if (!Number.isInteger(numericAge) || numericAge < 0 || numericAge > 120) {
      errors.travelerAge = "Enter an age from 0 to 120.";
    }
  }

  return errors;
}

export function buildJourneyRequest({ nationality, route, baggage, documents, includeDocumentDetails = true } = {}) {
  const profile = documents ?? {};
  const age = typeof profile.travelerAge === "string"
    ? profile.travelerAge.trim()
    : profile.travelerAge;
  const numericAge = Number(age);

  return {
    nationalityCountryCode: normalizeCountryCode(nationality),
    route: normalizeRoute(route),
    baggage,
    documents: includeDocumentDetails ? {
      residenceCountryCode: normalizeCountryCode(profile.residenceCountryCode),
      passportIssuingCountryCode: normalizeCountryCode(profile.passportIssuingCountryCode),
      passportExpiryDate: profile.passportExpiryDate?.trim?.() || null,
      departureDate: profile.departureDate?.trim?.() || null,
      travelPurpose: profile.travelPurpose?.trim?.().toUpperCase() || null,
      travelerAge: age !== "" && age !== null && age !== undefined && Number.isInteger(numericAge)
        ? numericAge
        : null,
      residencePermitCountryCodes: normalizeCountryCodeList(profile.residencePermitCountryCodes),
      visaCountryCodes: normalizeCountryCodeList(profile.visaCountryCodes),
    } : null,
  };
}
