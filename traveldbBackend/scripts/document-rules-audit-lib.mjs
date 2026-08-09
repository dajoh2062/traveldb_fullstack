const ISO_DATE = /^\d{4}-\d{2}-\d{2}$/;
const DATASET_VERSION = /^(\d{4}-\d{2}-\d{2})\.([1-9]\d*)$/;
const RULE_ID = /^[a-z0-9]+(?:-[a-z0-9]+)*$/;
const DECISION_KEY = /^[A-Z0-9]+(?:_[A-Z0-9]+)*$/;
const TOKEN = /^[A-Z0-9_]+$/;
const COUNTRY_CODE = /^[A-Z]{2}$/;

const SCOPES = new Set(["JOURNEY", "ENTRY", "TRANSIT"]);
const CATEGORIES = new Set([
  "TRAVEL_DOCUMENT",
  "PASSPORT_VALIDITY",
  "VISA",
  "ELECTRONIC_AUTHORIZATION",
  "TRANSIT_PERMISSION",
  "HEALTH",
  "ARRIVAL_FORM",
  "ONWARD_TRAVEL",
  "OTHER",
]);
const STATUSES = new Set(["REQUIRED", "NOT_REQUIRED", "CONDITIONAL", "VERIFY"]);

// This list deliberately contains only public-sector authorities used by this
// snapshot. Additions require a reviewer to confirm ownership before importing
// a source from a new domain.
export const OFFICIAL_SOURCE_HOST_SUFFIXES = Object.freeze([
  "cbp.gov",
  "canada.ca",
  "europa.eu",
  "gov.br",
  "gov.au",
  "gov.uk",
  "govt.nz",
  "mofa.go.jp",
  "fi.emb-japan.go.jp",
  "moj.go.jp",
  "regjeringen.no",
  "state.gov",
]);

export function auditDocumentRulesSnapshot(snapshot, options = {}) {
  const asOf = options.asOf;
  const maxSnapshotAgeDays = options.maxSnapshotAgeDays ?? 120;
  const maxReviewWindowDays = options.maxReviewWindowDays ?? 120;

  if (!isIsoDate(asOf)) {
    throw new TypeError("audit asOf must be a real date in YYYY-MM-DD format");
  }
  requirePositiveInteger(maxSnapshotAgeDays, "maxSnapshotAgeDays");
  requirePositiveInteger(maxReviewWindowDays, "maxReviewWindowDays");

  const errors = [];
  const warnings = [];
  if (!snapshot || typeof snapshot !== "object" || Array.isArray(snapshot)) {
    return result(["Snapshot must be a JSON object"], warnings, null);
  }

  if (snapshot.schemaVersion !== 2) errors.push("schemaVersion must be 2");

  const versionMatch = typeof snapshot.datasetVersion === "string"
    ? snapshot.datasetVersion.match(DATASET_VERSION)
    : null;
  if (!versionMatch) {
    errors.push("datasetVersion must use YYYY-MM-DD.N format");
  }

  const generatedAt = parseCanonicalInstant(snapshot.generatedAt);
  if (!generatedAt) {
    errors.push("generatedAt must be a canonical UTC timestamp such as 2026-07-30T12:00:00.000Z");
  } else {
    const generatedDate = generatedAt.toISOString().slice(0, 10);
    if (versionMatch && versionMatch[1] !== generatedDate) {
      errors.push("datasetVersion date must match generatedAt's UTC date");
    }
    if (generatedDate > asOf) {
      errors.push(`generatedAt is after audit date ${asOf}`);
    } else if (daysBetween(generatedDate, asOf) > maxSnapshotAgeDays) {
      errors.push(`snapshot is older than ${maxSnapshotAgeDays} days on ${asOf}`);
    }
  }

  validateSources(snapshot.sources, "sources", errors);

  if (!Array.isArray(snapshot.rules) || snapshot.rules.length === 0) {
    errors.push("rules must be a non-empty array");
    return result(errors, warnings, snapshot);
  }

  const ids = new Set();
  const reviewDates = [];
  for (const [index, rule] of snapshot.rules.entries()) {
    const path = `rules[${index}]`;
    if (!rule || typeof rule !== "object" || Array.isArray(rule)) {
      errors.push(`${path} must be an object`);
      continue;
    }

    if (typeof rule.id !== "string" || !RULE_ID.test(rule.id)) {
      errors.push(`${path}.id must be a stable lowercase kebab-case identifier`);
    } else if (ids.has(rule.id)) {
      errors.push(`duplicate rule id: ${rule.id}`);
    } else {
      ids.add(rule.id);
    }

    if (typeof rule.decisionKey !== "string" || !DECISION_KEY.test(rule.decisionKey)) {
      errors.push(`${label(rule, path)}.decisionKey must be an uppercase token`);
    }
    if (!SCOPES.has(rule.scope)) {
      errors.push(`${label(rule, path)}.scope must be JOURNEY, ENTRY or TRANSIT`);
    }
    validateTokenArray(rule.destinationCountries, `${label(rule, path)}.destinationCountries`, errors, {
      countryCodes: true,
      required: true,
      allowWildcard: true,
    });
    validateTokenArray(rule.nationalities, `${label(rule, path)}.nationalities`, errors, {
      countryCodes: true,
      required: true,
      allowWildcard: true,
    });
    for (const field of [
      "excludedNationalities",
      "residenceCountries",
      "passportIssuingCountries",
      "requiredHeldVisaCountries",
      "requiredResidencePermitCountries",
    ]) {
      if (rule[field] !== undefined) {
        validateTokenArray(rule[field], `${label(rule, path)}.${field}`, errors, {
          countryCodes: true,
          required: false,
          allowWildcard: field === "residenceCountries" || field === "passportIssuingCountries",
        });
      }
    }
    if (rule.travelPurposes !== undefined) {
      validateTokenArray(rule.travelPurposes, `${label(rule, path)}.travelPurposes`, errors, {
        required: false,
        allowWildcard: true,
      });
    }

    if (!Number.isInteger(rule.priority) || rule.priority < 0) {
      errors.push(`${label(rule, path)}.priority must be a non-negative integer`);
    }
    validateOptionalInteger(rule.minimumAge, `${label(rule, path)}.minimumAge`, errors);
    validateOptionalInteger(rule.maximumAge, `${label(rule, path)}.maximumAge`, errors);
    if (Number.isInteger(rule.minimumAge) && Number.isInteger(rule.maximumAge)
        && rule.minimumAge > rule.maximumAge) {
      errors.push(`${label(rule, path)} minimumAge must not exceed maximumAge`);
    }

    const effectiveFrom = validateOptionalDate(rule.effectiveFrom, `${label(rule, path)}.effectiveFrom`, errors);
    const effectiveTo = validateOptionalDate(rule.effectiveTo, `${label(rule, path)}.effectiveTo`, errors);
    const lastVerified = validateRequiredDate(rule.lastVerified, `${label(rule, path)}.lastVerified`, errors);
    const reviewAfter = validateRequiredDate(rule.reviewAfter, `${label(rule, path)}.reviewAfter`, errors);
    if (effectiveFrom && effectiveTo && effectiveFrom > effectiveTo) {
      errors.push(`${label(rule, path)} effectiveFrom must not be after effectiveTo`);
    }
    if (lastVerified && lastVerified > asOf) {
      errors.push(`${label(rule, path)} lastVerified is after audit date ${asOf}`);
    }
    if (lastVerified && reviewAfter) {
      if (reviewAfter < lastVerified) {
        errors.push(`${label(rule, path)} reviewAfter must not precede lastVerified`);
      } else if (daysBetween(lastVerified, reviewAfter) > maxReviewWindowDays) {
        errors.push(`${label(rule, path)} review window exceeds ${maxReviewWindowDays} days`);
      }
    }
    if (reviewAfter) {
      reviewDates.push(reviewAfter);
      if (reviewAfter < asOf) {
        errors.push(`${label(rule, path)} is past its review date (${reviewAfter})`);
      } else if (daysBetween(asOf, reviewAfter) <= 14) {
        warnings.push(`${label(rule, path)} reaches its review date within 14 days (${reviewAfter})`);
      }
    }

    validateOutput(rule.output, `${label(rule, path)}.output`, errors);
  }

  return result(errors, warnings, snapshot, reviewDates);
}

function validateOutput(output, path, errors) {
  if (!output || typeof output !== "object" || Array.isArray(output)) {
    errors.push(`${path} must be an object`);
    return;
  }
  if (typeof output.code !== "string" || !DECISION_KEY.test(output.code)) {
    errors.push(`${path}.code must be an uppercase token`);
  }
  if (!CATEGORIES.has(output.category)) {
    errors.push(`${path}.category is not supported`);
  }
  if (!STATUSES.has(output.status)) {
    errors.push(`${path}.status is not supported`);
  }
  for (const field of ["title", "summary"]) {
    if (typeof output[field] !== "string" || !output[field].trim()) {
      errors.push(`${path}.${field} must be non-empty text`);
    }
  }
  if (!Array.isArray(output.conditions)
      || output.conditions.some(value => typeof value !== "string" || !value.trim())) {
    errors.push(`${path}.conditions must be an array of non-empty strings`);
  }
  validateKeyFacts(output.keyFacts, `${path}.keyFacts`, errors);
  validateSources(output.sources, `${path}.sources`, errors);
}

function validateKeyFacts(keyFacts, path, errors) {
  if (keyFacts === undefined) return;
  if (!Array.isArray(keyFacts) || keyFacts.length > 6) {
    errors.push(`${path} must be an array with at most 6 facts`);
    return;
  }

  const labels = new Set();
  for (const [index, fact] of keyFacts.entries()) {
    const factPath = `${path}[${index}]`;
    if (!fact || typeof fact !== "object" || Array.isArray(fact)) {
      errors.push(`${factPath} must be an object`);
      continue;
    }
    for (const field of ["label", "value"]) {
      if (typeof fact[field] !== "string" || !fact[field].trim()) {
        errors.push(`${factPath}.${field} must be non-empty text`);
      }
    }
    if (typeof fact.label === "string" && fact.label.trim()) {
      const normalizedLabel = fact.label.trim().toLowerCase();
      if (labels.has(normalizedLabel)) errors.push(`${path} contains duplicate label: ${fact.label.trim()}`);
      labels.add(normalizedLabel);
    }
  }
}

function validateSources(sources, path, errors) {
  if (!Array.isArray(sources) || sources.length === 0) {
    errors.push(`${path} must contain at least one official source`);
    return;
  }
  const urls = new Set();
  for (const [index, source] of sources.entries()) {
    const sourcePath = `${path}[${index}]`;
    if (!source || typeof source !== "object" || Array.isArray(source)) {
      errors.push(`${sourcePath} must be an object`);
      continue;
    }
    if (typeof source.label !== "string" || !source.label.trim()) {
      errors.push(`${sourcePath}.label must be non-empty text`);
    }
    if (source.sourceType !== "GOVERNMENT") {
      errors.push(`${sourcePath}.sourceType must be GOVERNMENT`);
    }
    let parsed;
    try {
      parsed = new URL(source.url);
    } catch {
      errors.push(`${sourcePath}.url must be an absolute URL`);
      continue;
    }
    if (parsed.protocol !== "https:") {
      errors.push(`${sourcePath}.url must use HTTPS`);
    }
    if (parsed.username || parsed.password) {
      errors.push(`${sourcePath}.url must not contain credentials`);
    }
    if (!isOfficialSourceHost(parsed.hostname)) {
      errors.push(`${sourcePath}.url host is not in the reviewed authority allowlist: ${parsed.hostname}`);
    }
    if (urls.has(parsed.href)) {
      errors.push(`${path} contains duplicate source URL: ${parsed.href}`);
    }
    urls.add(parsed.href);
  }
}

function validateTokenArray(value, path, errors, options) {
  if (!Array.isArray(value) || (options.required && value.length === 0)) {
    errors.push(`${path} must be ${options.required ? "a non-empty" : "an"} array`);
    return;
  }
  const seen = new Set();
  for (const token of value) {
    const valid = typeof token === "string"
      && (options.allowWildcard && token === "*"
        || options.countryCodes && COUNTRY_CODE.test(token)
        || !options.countryCodes && TOKEN.test(token));
    if (!valid) errors.push(`${path} contains an invalid token: ${String(token)}`);
    if (seen.has(token)) errors.push(`${path} contains a duplicate token: ${String(token)}`);
    seen.add(token);
  }
  if (value.includes("*") && value.length > 1) {
    errors.push(`${path} must not combine * with specific values`);
  }
}

function validateOptionalInteger(value, path, errors) {
  if (value !== undefined && (!Number.isInteger(value) || value < 0)) {
    errors.push(`${path} must be a non-negative integer when present`);
  }
}

function validateOptionalDate(value, path, errors) {
  if (value === undefined || value === null || value === "") return null;
  if (!isIsoDate(value)) {
    errors.push(`${path} must be a real date in YYYY-MM-DD format`);
    return null;
  }
  return value;
}

function validateRequiredDate(value, path, errors) {
  if (!isIsoDate(value)) {
    errors.push(`${path} must be a real date in YYYY-MM-DD format`);
    return null;
  }
  return value;
}

function isOfficialSourceHost(hostname) {
  const normalized = hostname.toLowerCase().replace(/\.$/, "");
  return OFFICIAL_SOURCE_HOST_SUFFIXES.some(suffix =>
    normalized === suffix || normalized.endsWith(`.${suffix}`),
  );
}

function parseCanonicalInstant(value) {
  if (typeof value !== "string") return null;
  const parsed = new Date(value);
  if (Number.isNaN(parsed.valueOf())) return null;
  const canonical = parsed.toISOString();
  return value === canonical || value === canonical.replace(".000Z", "Z") ? parsed : null;
}

function isIsoDate(value) {
  if (typeof value !== "string" || !ISO_DATE.test(value)) return false;
  const parsed = new Date(`${value}T00:00:00.000Z`);
  return !Number.isNaN(parsed.valueOf()) && parsed.toISOString().slice(0, 10) === value;
}

function daysBetween(start, end) {
  return Math.round((Date.parse(`${end}T00:00:00.000Z`) - Date.parse(`${start}T00:00:00.000Z`)) / 86_400_000);
}

function requirePositiveInteger(value, name) {
  if (!Number.isInteger(value) || value <= 0) {
    throw new TypeError(`${name} must be a positive integer`);
  }
}

function label(rule, fallback) {
  return typeof rule.id === "string" && rule.id ? `rule ${rule.id}` : fallback;
}

function result(errors, warnings, snapshot, reviewDates = []) {
  return {
    errors: [...errors].sort(),
    warnings: [...warnings].sort(),
    summary: {
      datasetVersion: typeof snapshot?.datasetVersion === "string" ? snapshot.datasetVersion : null,
      ruleCount: Array.isArray(snapshot?.rules) ? snapshot.rules.length : 0,
      snapshotSourceCount: Array.isArray(snapshot?.sources) ? snapshot.sources.length : 0,
      nextReviewAfter: reviewDates.length ? [...reviewDates].sort()[0] : null,
    },
  };
}
