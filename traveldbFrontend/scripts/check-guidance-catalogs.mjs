import { readFile, readdir } from "node:fs/promises";
import { dirname, join } from "node:path";
import { fileURLToPath } from "node:url";
import { SUPPORTED_LOCALE_CODES } from "../src/i18n/locales.js";

const frontendDirectory = dirname(dirname(fileURLToPath(import.meta.url)));
const guidanceDirectory = join(frontendDirectory, "src", "i18n", "guidance");
const snapshotPath = join(
  frontendDirectory,
  "..",
  "traveldbBackend",
  "src",
  "main",
  "resources",
  "data",
  "document-rules.json",
);
const snapshot = JSON.parse(await readFile(snapshotPath, "utf8"));
const englishCatalog = await readCatalog("en-GB");
const englishPaths = leafPaths(englishCatalog);
const englishEntries = new Map(leafEntries(englishCatalog));

assert(
  englishCatalog.documentDatasetVersion === snapshot.datasetVersion,
  "English guidance dataset version does not match the backend snapshot.",
);
for (const rule of snapshot.rules) {
  const actual = englishCatalog.documents.rules[rule.id];
  const expected = {
    title: rule.output.title,
    summary: rule.output.summary,
    conditions: rule.output.conditions,
    keyFacts: rule.output.keyFacts,
  };
  assert(actual, `English guidance is missing document rule ${rule.id}.`);
  assert(
    JSON.stringify(actual) === JSON.stringify(expected),
    `English guidance for ${rule.id} differs from the backend snapshot.`,
  );
}

const files = await readdir(guidanceDirectory);
const jsonCatalogs = files.filter(file => file.endsWith(".json"));
assert(
  jsonCatalogs.length === SUPPORTED_LOCALE_CODES.length,
  `Expected ${SUPPORTED_LOCALE_CODES.length} guidance catalogs, found ${jsonCatalogs.length}.`,
);
assert(
  files.every(file => file.endsWith(".json")),
  "Guidance catalogs must be JSON files.",
);

for (const locale of SUPPORTED_LOCALE_CODES) {
  const catalog = await readCatalog(locale);
  assert(
    JSON.stringify(leafPaths(catalog)) === JSON.stringify(englishPaths),
    `${locale} guidance structure differs from en-GB.`,
  );
  assert(
    leafValues(catalog).every(value => typeof value !== "string" || value.trim()),
    `${locale} guidance contains a blank value.`,
  );
  if (!locale.startsWith("en")) {
    const unchanged = leafEntries(catalog).filter(
      ([path, value]) =>
        path !== "documentDatasetVersion" &&
        typeof value === "string" &&
        value === englishEntries.get(path),
    );
    assert(
      unchanged.length / englishEntries.size < 0.1,
      `${locale} guidance still matches English in ${unchanged.length} fields.`,
    );
  }
}

console.log(`Validated ${SUPPORTED_LOCALE_CODES.length} guidance catalogs.`);

async function readCatalog(locale) {
  return JSON.parse(await readFile(join(guidanceDirectory, `${locale}.json`), "utf8"));
}

function leafPaths(value, prefix = "") {
  if (Array.isArray(value)) {
    return value.flatMap((item, index) => leafPaths(item, `${prefix}.${index}`));
  }
  if (value && typeof value === "object") {
    return Object.entries(value).flatMap(([key, item]) =>
      leafPaths(item, prefix ? `${prefix}.${key}` : key),
    );
  }
  return [prefix];
}

function leafEntries(value, prefix = "") {
  if (Array.isArray(value)) {
    return value.flatMap((item, index) => leafEntries(item, `${prefix}.${index}`));
  }
  if (value && typeof value === "object") {
    return Object.entries(value).flatMap(([key, item]) =>
      leafEntries(item, prefix ? `${prefix}.${key}` : key),
    );
  }
  return [[prefix, value]];
}

function leafValues(value) {
  if (Array.isArray(value)) return value.flatMap(leafValues);
  if (value && typeof value === "object") return Object.values(value).flatMap(leafValues);
  return [value];
}

function assert(condition, message) {
  if (!condition) throw new Error(message);
}
