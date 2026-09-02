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

export function sortDocumentRequirementsByCountry(requirements = [], route = []) {
  const countryRouteOrder = new Map();
  for (const [index, airport] of route.entries()) {
    const countryCode = normalizedCountryCode(airport.countryCode);
    if (countryCode && !countryRouteOrder.has(countryCode)) {
      countryRouteOrder.set(countryCode, index);
    }
  }

  return requirements
    .map((requirement, index) => ({
      requirement,
      index,
      countryCode: documentCountryCode(requirement, route),
    }))
    .sort((left, right) => {
      const leftCountryOrder = countryOrder(left.countryCode, countryRouteOrder);
      const rightCountryOrder = countryOrder(right.countryCode, countryRouteOrder);
      if (leftCountryOrder !== rightCountryOrder) return leftCountryOrder - rightCountryOrder;
      if (left.countryCode !== right.countryCode) {
        return left.countryCode.localeCompare(right.countryCode);
      }
      return left.index - right.index;
    })
    .map(({ requirement }) => requirement);
}

export function groupDocumentRequirementsByCountry(requirements = [], route = []) {
  const destinationCountries = [];
  const seenDestinationCountries = new Set();

  for (const airport of route.slice(1)) {
    const countryCode = normalizedCountryCode(airport.countryCode);
    if (countryCode && !seenDestinationCountries.has(countryCode)) {
      seenDestinationCountries.add(countryCode);
      destinationCountries.push(countryCode);
    }
  }

  const groups = new Map();
  const addToGroup = (countryCode, requirement) => {
    if (!groups.has(countryCode)) {
      groups.set(countryCode, []);
    }
    groups.get(countryCode).push(requirement);
  };

  for (const requirement of requirements) {
    const countryCode = documentCountryCode(requirement, route);
    if (countryCode) {
      addToGroup(countryCode, requirement);
    } else if (requirement.scope === "JOURNEY" && destinationCountries.length > 0) {
      destinationCountries.forEach(destinationCountry =>
        addToGroup(destinationCountry, requirement),
      );
    } else {
      addToGroup("", requirement);
    }
  }

  const routeOrder = new Map(
    destinationCountries.map((countryCode, index) => [countryCode, index]),
  );
  return [...groups.entries()]
    .sort(([leftCountry], [rightCountry]) => {
      const leftOrder = countryOrder(leftCountry, routeOrder);
      const rightOrder = countryOrder(rightCountry, routeOrder);
      if (leftOrder !== rightOrder) return leftOrder - rightOrder;
      return leftCountry.localeCompare(rightCountry);
    })
    .map(([countryCode, countryRequirements]) => ({
      countryCode,
      requirements: countryRequirements,
    }));
}

export function documentCountryName(countryCode, locale = "en") {
  if (!countryCode) return null;

  try {
    return new Intl.DisplayNames([locale], { type: "region" }).of(countryCode) ?? countryCode;
  } catch {
    return countryCode;
  }
}

export function documentRequirementKey(requirement) {
  return [
    requirement.ruleId ?? requirement.code ?? requirement.title,
    requirement.scope ?? "JOURNEY",
    requirement.airportCode ?? requirement.countryCode ?? "ALL",
  ].join(":");
}

function documentCountryCode(requirement, route) {
  const explicitCountryCode = normalizedCountryCode(requirement.countryCode);
  if (explicitCountryCode) return explicitCountryCode;

  const airport = route.find(routeAirport => routeAirport.iataCode === requirement.airportCode);
  return normalizedCountryCode(airport?.countryCode);
}

function normalizedCountryCode(countryCode) {
  return typeof countryCode === "string" ? countryCode.trim().toUpperCase() : "";
}

function countryOrder(countryCode, countryRouteOrder) {
  if (!countryCode) return -1;
  return countryRouteOrder.get(countryCode) ?? Number.MAX_SAFE_INTEGER;
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
  return `${t("results.documents.notConfirmed")}: ${formattedInputs}.`;
}
