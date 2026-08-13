import { airportLabel } from "../../utils/journey";

const DOCUMENT_STATUSES = new Set(["REQUIRED", "CONDITIONAL", "VERIFY", "NOT_REQUIRED"]);
const INTERNAL_CONDITION = /^Local rule .+ was last verified \d{4}-\d{2}-\d{2}\.?$/i;
export function actionableBaggageStops(stops = []) {
  return stops.filter(stop => stop.status === "REQUIRED" || stop.status === "CONFIRM");
}

export function visibleDocumentRequirements(requirements = []) {
  const seen = new Set();

  return requirements.filter(requirement => {
    if (!DOCUMENT_STATUSES.has(requirement.status) || requirement.code === "ENTRY_CONDITIONS") {
      return false;
    }

    const key = documentRequirementKey(requirement);
    if (seen.has(key)) return false;

    seen.add(key);
    return true;
  });
}

export function documentReviewCount(requirements) {
  return requirements.filter(requirement => requirement.status !== "NOT_REQUIRED").length;
}

export function documentRequirementKey(requirement) {
  return [
    requirement.code ?? requirement.title,
    requirement.scope ?? "JOURNEY",
    requirement.airportCode ?? requirement.countryCode ?? "ALL",
  ].join(":");
}

export function documentLocation(requirement, route, t = key => key) {
  if (!requirement.airportCode) return t("results.documents.locations.wholeJourney");

  const location = airportLabel(requirement.airportCode, route);
  if (requirement.scope === "TRANSIT") {
    return t("results.documents.locations.transit", { location });
  }
  if (requirement.scope === "ENTRY") {
    return t("results.documents.locations.entry", { location });
  }
  return location;
}

export function documentSources(requirement) {
  const sources = requirement.sources ?? [];
  const governmentSources = sources.filter(source => source.sourceType === "GOVERNMENT");
  return (governmentSources.length > 0 ? governmentSources : sources).slice(0, 2);
}

export function documentKeyFacts(requirement) {
  return (requirement.keyFacts ?? [])
    .filter(
      fact =>
        typeof fact?.label === "string" &&
        fact.label.trim() &&
        typeof fact?.value === "string" &&
        fact.value.trim(),
    )
    .slice(0, 6);
}

export function documentConditions(requirement) {
  return (requirement.conditions ?? []).filter(
    condition =>
      typeof condition === "string" &&
      condition.trim() &&
      !INTERNAL_CONDITION.test(condition.trim()),
  );
}

export function formatRuleDate(value, locale = "en-GB") {
  const match = /^(\d{4})-(\d{2})-(\d{2})$/.exec(value ?? "");
  if (!match) return null;

  const year = Number(match[1]);
  const month = Number(match[2]);
  const day = Number(match[3]);
  if (month < 1 || month > 12 || day < 1 || day > 31) return null;

  const date = new Date(Date.UTC(year, month - 1, day));
  if (
    date.getUTCFullYear() !== year ||
    date.getUTCMonth() !== month - 1 ||
    date.getUTCDate() !== day
  ) {
    return null;
  }

  return new Intl.DateTimeFormat(locale, {
    day: "numeric",
    month: "short",
    year: "numeric",
    timeZone: "UTC",
  }).format(date);
}

const MISSING_INPUT_KEYS = {
  "Country of residence": "countryOfResidence",
  "Passport issuing country": "passportIssuingCountry",
  "Passport expiry date": "passportExpiryDate",
  "Primary travel document expiry date": "primaryDocumentExpiryDate",
  "Departure date": "departureDate",
  "Travel purpose": "travelPurpose",
};

export function missingInputMessage(missingInputs = [], t = key => key, locale = "en-GB") {
  const inputs = [
    ...new Set(
      missingInputs
        .filter(input => typeof input === "string" && input.trim())
        .map(input => {
          const normalized = input.trim();
          const key = MISSING_INPUT_KEYS[normalized];
          return key
            ? t(`results.documents.missingInputLabels.${key}`)
            : `${normalized.charAt(0).toLowerCase()}${normalized.slice(1)}`;
        }),
    ),
  ];
  if (inputs.length === 0) return null;

  const formattedInputs = new Intl.ListFormat(locale, {
    style: "long",
    type: "conjunction",
  }).format(inputs);
  return t("results.documents.missingInputs", {
    count: inputs.length,
    details: formattedInputs,
  });
}

export function baggageSources(stop) {
  return (stop.sources ?? []).slice(0, 2);
}

export function sourceLabel(source, sourceCount, t = key => key) {
  if (source.label?.includes("eVisitor")) return "eVisitor 651";
  if (source.label?.includes("Electronic Travel Authority")) return "ETA 601";
  if (source.label) return source.label;
  return sourceCount === 1 ? t("common.supportingSource") : t("common.source");
}
