import { readFile } from "node:fs/promises";
import { resolve } from "node:path";
import process from "node:process";
import { auditDocumentRulesSnapshot } from "./document-rules-audit-lib.mjs";

const DEFAULT_INPUT = "src/main/resources/data/document-rules.json";

async function main(args) {
  const options = parseOptions(args);
  if (options.help) {
    printUsage();
    return;
  }
  if (!options.asOf) {
    printUsage();
    throw new Error("--as-of is required so freshness checks are deterministic");
  }

  const inputPath = resolve(options.input ?? DEFAULT_INPUT);
  const snapshot = JSON.parse(await readFile(inputPath, "utf8"));
  const audit = auditDocumentRulesSnapshot(snapshot, {
    asOf: options.asOf,
    maxSnapshotAgeDays: options.maxSnapshotAgeDays,
    maxReviewWindowDays: options.maxReviewWindowDays,
  });

  for (const warning of audit.warnings) {
    console.warn(`WARN: ${warning}`);
  }
  for (const error of audit.errors) {
    console.error(`ERROR: ${error}`);
  }

  if (audit.errors.length > 0) {
    console.error(`Document-rule audit failed with ${audit.errors.length} error(s).`);
    process.exitCode = 1;
    return;
  }

  console.log(
    `Document-rule audit passed: ${audit.summary.ruleCount} rules, `
      + `${audit.summary.snapshotSourceCount} snapshot sources, `
      + `dataset ${audit.summary.datasetVersion}.`,
  );
  console.log(`Next rule review date: ${audit.summary.nextReviewAfter}`);
}

function parseOptions(args) {
  const options = {
    input: null,
    asOf: null,
    maxSnapshotAgeDays: 120,
    maxReviewWindowDays: 120,
    help: false,
  };

  for (let index = 0; index < args.length; index += 1) {
    const option = args[index];
    switch (option) {
      case "--help":
      case "-h":
        options.help = true;
        break;
      case "--input":
        options.input = readOptionValue(args, ++index, option);
        break;
      case "--as-of":
        options.asOf = readOptionValue(args, ++index, option);
        break;
      case "--max-snapshot-age-days":
        options.maxSnapshotAgeDays = positiveInteger(
          readOptionValue(args, ++index, option),
          option,
        );
        break;
      case "--max-review-window-days":
        options.maxReviewWindowDays = positiveInteger(
          readOptionValue(args, ++index, option),
          option,
        );
        break;
      default:
        throw new Error(`Unknown option: ${option}`);
    }
  }

  return options;
}

function readOptionValue(args, index, option) {
  const value = args[index];
  if (!value || value.startsWith("--")) {
    throw new Error(`Missing value for ${option}`);
  }
  return value;
}

function positiveInteger(value, option) {
  const parsed = Number(value);
  if (!Number.isInteger(parsed) || parsed <= 0) {
    throw new Error(`${option} must be a positive integer`);
  }
  return parsed;
}

function printUsage() {
  console.log(
    "Usage: node scripts/audit-document-rules.mjs --as-of YYYY-MM-DD "
      + "[--input <file>] [--max-snapshot-age-days 120] [--max-review-window-days 120]",
  );
}

try {
  await main(process.argv.slice(2));
} catch (error) {
  console.error(`Document-rule audit failed: ${error.message}`);
  process.exitCode = 1;
}
