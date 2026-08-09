import {
  createTravelDocument,
  getTravelDocumentType,
  isOrdinaryPassport,
  MAX_TRAVEL_DOCUMENTS,
} from "./travelDocuments";

export function createInitialDocumentProfile() {
  return {
    residenceCountryCode: "",
    departureDate: "",
    travelPurpose: "TOURISM",
    travelerAge: "",
    travelDocuments: [createTravelDocument({ primary: true })],
  };
}

export const initialDocumentProfile = createInitialDocumentProfile();

export const initialBaggageProfile = {
  checkedBaggage: true,
  ticketArrangement: "SINGLE_BOOKING",
  checkedThrough: "YES",
};

const BASIC_TRAVEL_PURPOSE = "TOURISM";

const countryCodePattern = /^[A-Za-z]{2}$/;
const isoDatePattern = /^\d{4}-\d{2}-\d{2}$/;
const documentFieldKeys = new Set([
  "residenceCountryCode",
  "departureDate",
  "travelPurpose",
  "travelerAge",
  "travelDocuments",
  // Remove errors returned by older backend versions when advanced search is hidden.
  "passportIssuingCountryCode",
  "passportExpiryDate",
  "residencePermitCountryCodes",
  "visaCountryCodes",
]);

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
  return [...new Set(values.filter(Boolean))];
}

function normalizeRoute(route) {
  if (!Array.isArray(route)) return [];

  return route
    .map(stop => typeof stop === "string" ? stop : stop?.iataCode)
    .filter(code => typeof code === "string" && code.trim())
    .map(code => code.trim().toUpperCase());
}

function normalizeTravelDocuments(documents) {
  if (!Array.isArray(documents)) return [];

  return documents
    .filter(document => document && typeof document === "object")
    .map(document => {
      const type = typeof document.type === "string"
        ? document.type.trim().toUpperCase()
        : "";
      const issuingCountryCode = normalizeCountryCode(document.issuingCountryCode);
      const expiryDate = document.expiryDate?.trim?.() ?? "";
      const customType = document.customType?.trim?.() ?? "";

      return {
        type,
        customType: type === "OTHER" && customType ? customType : null,
        issuingCountryCode: countryCodePattern.test(issuingCountryCode)
          ? issuingCountryCode
          : null,
        expiryDate: expiryDate || null,
        primary: document.primary === true,
      };
    });
}

function legacyDocumentErrorPath(field, profile) {
  const documents = Array.isArray(profile?.travelDocuments) ? profile.travelDocuments : [];

  if (field === "passportIssuingCountryCode" || field === "passportExpiryDate") {
    const primaryIndex = documents.findIndex(document => (
      document?.primary && isOrdinaryPassport(document.type)
    ));
    if (primaryIndex >= 0) {
      const itemField = field === "passportExpiryDate" ? "expiryDate" : "issuingCountryCode";
      return `travelDocuments.${primaryIndex}.${itemField}`;
    }
    return "travelDocuments._error";
  }

  const legacyListMatch = field.match(/^(visaCountryCodes|residencePermitCountryCodes)(?:\[(\d+)\])?/);
  if (legacyListMatch) {
    const type = legacyListMatch[1] === "visaCountryCodes" ? "VISA" : "RESIDENCE_PERMIT";
    const matchingIndices = documents
      .map((document, index) => document?.type === type ? index : -1)
      .filter(index => index >= 0);
    const requestedIndex = Number(legacyListMatch[2] ?? 0);
    if (matchingIndices[requestedIndex] !== undefined) {
      return `travelDocuments.${matchingIndices[requestedIndex]}.issuingCountryCode`;
    }
    return "travelDocuments._error";
  }

  return field.split("[")[0];
}

function formFieldPath(apiField = "", profile) {
  if (apiField === "nationalityCountryCode") return "nationality";
  if (apiField === "route" || apiField.startsWith("route[")) return "route";
  if (apiField === "documents") return "travelDocuments._error";
  if (!apiField.startsWith("documents.")) return apiField;

  const documentField = apiField.slice("documents.".length);
  if (documentField === "travelDocuments") return "travelDocuments._error";
  if (documentField.startsWith("travelDocuments[")) {
    const path = documentField.replace(/\[(\d+)\]/g, ".$1");
    return /^travelDocuments\.\d+$/.test(path) ? `${path}._error` : path;
  }
  return legacyDocumentErrorPath(documentField, profile);
}

function setFieldError(errors, path, message) {
  if (!path) return;

  const segments = path.split(".");
  let current = errors;
  segments.forEach((segment, index) => {
    if (index === segments.length - 1) {
      current[segment] = message;
      return;
    }
    current[segment] ??= {};
    current = current[segment];
  });
}

export function mapApiErrorsToFields(errors = [], profile) {
  const mappedErrors = {};
  errors.forEach(error => {
    setFieldError(mappedErrors, formFieldPath(error.field, profile), error.message);
  });
  return mappedErrors;
}

export function removeDocumentFieldErrors(errors) {
  return Object.fromEntries(
    Object.entries(errors).filter(([field]) => !documentFieldKeys.has(field)),
  );
}

function validateTravelDocuments(profile, departureDate) {
  const documents = Array.isArray(profile.travelDocuments) ? profile.travelDocuments : [];
  const errors = {};

  if (documents.length === 0) {
    errors._error = "Add at least one travel document.";
    return errors;
  }
  if (documents.length > MAX_TRAVEL_DOCUMENTS) {
    errors._error = `Add no more than ${MAX_TRAVEL_DOCUMENTS} travel documents.`;
  }

  const primaryCount = documents.filter(document => document?.primary === true).length;
  if (primaryCount !== 1) {
    errors._error = "Choose one primary document for this trip.";
  }

  documents.forEach((document, index) => {
    const itemErrors = {};
    const typeDetails = getTravelDocumentType(document?.type);
    const customType = document?.customType?.trim?.() ?? "";
    const issuingCountryCode = document?.issuingCountryCode?.trim?.() ?? "";
    const expiryDate = document?.expiryDate?.trim?.() ?? "";

    if (!typeDetails) {
      itemErrors.type = "Select a supported document type.";
    }
    if (document?.type === "OTHER" && !customType) {
      itemErrors.customType = "Enter a name for this document.";
    } else if (customType.length > 80) {
      itemErrors.customType = "Use 80 characters or fewer.";
    }

    if (typeDetails?.issuerRequired && !issuingCountryCode) {
      itemErrors.issuingCountryCode = "Select the issuing country.";
    } else if (issuingCountryCode && !isCountryCode(issuingCountryCode)) {
      itemErrors.issuingCountryCode = "Select a valid issuing country.";
    }

    if (expiryDate && !isIsoDate(expiryDate)) {
      itemErrors.expiryDate = "Enter a valid expiry date.";
    } else if (expiryDate && isIsoDate(departureDate) && expiryDate < departureDate) {
      itemErrors.expiryDate = "This document expires before departure.";
    } else if (expiryDate && expiryDate < localToday()) {
      itemErrors.expiryDate = "This document has expired.";
    }

    if (Object.keys(itemErrors).length > 0) errors[index] = itemErrors;
  });

  return errors;
}

export function validateJourneyForm({ nationality, route, documents, includeDocumentDetails = true } = {}) {
  const errors = {};

  if (!isCountryCode(nationality)) {
    errors.nationality = "Select the traveller's nationality from the search results.";
  }

  if (normalizeRoute(route).length < 2) {
    errors.route = "Add at least an origin and a destination.";
  }

  if (!includeDocumentDetails) return errors;

  const profile = documents ?? {};
  const departureDate = profile.departureDate?.trim?.() ?? "";
  const travelPurpose = profile.travelPurpose?.trim?.() ?? "";
  const travelerAge = typeof profile.travelerAge === "string"
    ? profile.travelerAge.trim()
    : profile.travelerAge;

  if (!isCountryCode(profile.residenceCountryCode)) {
    errors.residenceCountryCode = "Select your country of residence.";
  }

  if (!departureDate) {
    errors.departureDate = "Enter your departure date.";
  } else if (!isIsoDate(departureDate)) {
    errors.departureDate = "Enter a valid departure date.";
  } else if (departureDate < localToday()) {
    errors.departureDate = "Departure date cannot be in the past.";
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

  const travelDocumentErrors = validateTravelDocuments(profile, departureDate);
  if (Object.keys(travelDocumentErrors).length > 0) {
    errors.travelDocuments = travelDocumentErrors;
  }

  return errors;
}

export function buildJourneyRequest({ nationality, route, baggage, documents, includeDocumentDetails = true } = {}) {
  const profile = documents ?? {};
  const age = typeof profile.travelerAge === "string"
    ? profile.travelerAge.trim()
    : profile.travelerAge;
  const numericAge = Number(age);
  const travelDocuments = normalizeTravelDocuments(profile.travelDocuments);
  const primaryPassport = travelDocuments.find(document => (
    document.primary && isOrdinaryPassport(document.type)
  ));

  return {
    nationalityCountryCode: normalizeCountryCode(nationality),
    route: normalizeRoute(route),
    baggage,
    documents: includeDocumentDetails ? {
      residenceCountryCode: normalizeCountryCode(profile.residenceCountryCode),
      passportIssuingCountryCode: primaryPassport?.issuingCountryCode ?? null,
      passportExpiryDate: primaryPassport?.expiryDate ?? null,
      departureDate: profile.departureDate?.trim?.() || null,
      travelPurpose: profile.travelPurpose?.trim?.().toUpperCase() || null,
      travelerAge: age !== "" && age !== null && age !== undefined && Number.isInteger(numericAge)
        ? numericAge
        : null,
      residencePermitCountryCodes: normalizeCountryCodeList(
        travelDocuments
          .filter(document => document.type === "RESIDENCE_PERMIT")
          .map(document => document.issuingCountryCode),
      ),
      visaCountryCodes: normalizeCountryCodeList(
        travelDocuments
          .filter(document => document.type === "VISA")
          .map(document => document.issuingCountryCode),
      ),
      travelDocuments,
    } : {
      travelPurpose: BASIC_TRAVEL_PURPOSE,
    },
  };
}
