import { airportLabel } from "../../utils/journey";

const DOCUMENT_STATUSES = new Set(["REQUIRED", "CONDITIONAL", "VERIFY", "NOT_REQUIRED"]);
const INTERNAL_CONDITION = /^Local rule .+ was last verified \d{4}-\d{2}-\d{2}\.?$/i;
const MONTH_NAMES = [
  "Jan",
  "Feb",
  "Mar",
  "Apr",
  "May",
  "Jun",
  "Jul",
  "Aug",
  "Sep",
  "Oct",
  "Nov",
  "Dec",
];

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

export function documentLocation(requirement, route) {
  if (!requirement.airportCode) return "Whole journey";

  const prefix =
    requirement.scope === "TRANSIT"
      ? "Transit at"
      : requirement.scope === "ENTRY"
        ? "Entry at"
        : null;
  const location = airportLabel(requirement.airportCode, route);
  return prefix ? `${prefix} ${location}` : location;
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

export function formatRuleDate(value) {
  const match = /^(\d{4})-(\d{2})-(\d{2})$/.exec(value ?? "");
  if (!match) return null;

  const month = Number(match[2]);
  if (month < 1 || month > 12) return null;
  return `${Number(match[3])} ${MONTH_NAMES[month - 1]} ${match[1]}`;
}

export function missingInputMessage(missingInputs = []) {
  const inputs = [
    ...new Set(
      missingInputs
        .filter(input => typeof input === "string" && input.trim())
        .map(input => `${input.trim().charAt(0).toLowerCase()}${input.trim().slice(1)}`),
    ),
  ];
  if (inputs.length === 0) return null;

  const formattedInputs = new Intl.ListFormat("en", {
    style: "long",
    type: "conjunction",
  }).format(inputs);
  const detailReference = inputs.length === 1 ? "this detail" : "these details";
  return `Not included: ${formattedInputs}. Verify ${detailReference} in the linked official guidance.`;
}

export function baggageSources(stop) {
  return (stop.sources ?? []).slice(0, 2);
}

export function sourceLabel(source, sourceCount) {
  if (source.label?.includes("eVisitor")) return "eVisitor 651";
  if (source.label?.includes("Electronic Travel Authority")) return "ETA 601";
  if (source.label) return source.label;
  return sourceCount === 1 ? "Supporting source" : "Source";
}
