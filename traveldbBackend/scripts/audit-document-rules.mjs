import { readFile } from "node:fs/promises";
import { resolve } from "node:path";
import process from "node:process";
import { auditDocumentRulesSnapshot } from "./document-rules-audit-lib.mjs";

const options = parseOptions(process.argv.slice(2));
if (options.help) {
  printUsage();
  process.exit(0);
}
if (!options.asOf) {
  printUsage();
  throw new Error("--as-of is required so freshness checks are deterministic");
}

const input = resolve(options.input ?? "src/main/resources/data/document-rules.json");
const snapshot = JSON.parse(await readFile(input, "utf8"));
const audit = auditDocumentRulesSnapshot(snapshot, {
  asOf: options.asOf,
  maxSnapshotAgeDays: options.maxSnapshotAgeDays,
  maxReviewWindowDays: options.maxReviewWindowDays,
});

for (const warning of audit.warnings) console.warn(`WARN: ${warning}`);
for (const error of audit.errors) console.error(`ERROR: ${error}`);

if (audit.errors.length > 0) {
  console.error(`Document-rule audit failed with ${audit.errors.length} error(s).`);
  process.exitCode = 1;
} else {
  console.log(
    `Document-rule audit passed: ${audit.summary.ruleCount} rules, `
      + `${audit.summary.snapshotSourceCount} snapshot sources, `
      + `dataset ${audit.summary.datasetVersion}.`,
  );
  console.log(`Next rule review date: ${audit.summary.nextReviewAfter}`);
}

function parseOptions(args) {
  const parsed = {
    input: null,
    asOf: null,
    maxSnapshotAgeDays: 120,
    maxReviewWindowDays: 120,
    help: false,
  };
  for (let index = 0; index < args.length; index += 1) {
    const option = args[index];
    if (option === "--help" || option === "-h") {
      parsed.help = true;
      continue;
    }
    const value = args[index + 1];
    if (!value || value.startsWith("--")) throw new Error(`Missing value for ${option}`);
    index += 1;
    if (option === "--input") parsed.input = value;
    else if (option === "--as-of") parsed.asOf = value;
    else if (option === "--max-snapshot-age-days") parsed.maxSnapshotAgeDays = positiveInteger(value, option);
    else if (option === "--max-review-window-days") parsed.maxReviewWindowDays = positiveInteger(value, option);
    else throw new Error(`Unknown option: ${option}`);
  }
  return parsed;
}

function positiveInteger(value, option) {
  const parsed = Number(value);
  if (!Number.isInteger(parsed) || parsed <= 0) throw new Error(`${option} must be a positive integer`);
  return parsed;
}

function printUsage() {
  console.log(
    "Usage: node scripts/audit-document-rules.mjs --as-of YYYY-MM-DD "
      + "[--input <file>] [--max-snapshot-age-days 120] [--max-review-window-days 120]",
  );
}
