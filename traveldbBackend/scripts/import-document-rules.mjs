import { mkdir, readFile, writeFile } from "node:fs/promises";
import { dirname, resolve } from "node:path";
import process from "node:process";

const DEFAULT_OUTPUT = "src/main/resources/data/document-rules.json";

async function main(args) {
  const options = parseOptions(args);
  if (options.help) {
    printUsage();
    return;
  }
  if (!options.input) {
    printUsage();
    throw new Error("--input is required");
  }

  const rawSnapshot = await readInput(options.input);
  const snapshot = JSON.parse(rawSnapshot);
  validateSnapshot(snapshot);

  const sortedSnapshot = {
    ...snapshot,
    rules: [...snapshot.rules].sort(compareRules),
  };
  const outputPath = resolve(options.output ?? DEFAULT_OUTPUT);

  await mkdir(dirname(outputPath), { recursive: true });
  await writeFile(outputPath, `${JSON.stringify(sortedSnapshot, null, 2)}\n`, "utf8");

  console.log(`Imported ${sortedSnapshot.rules.length} document rules from ${options.input}`);
  console.log(`Dataset ${sortedSnapshot.datasetVersion} written to ${outputPath}`);
}

function parseOptions(args) {
  const options = {
    input: null,
    output: null,
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
      case "--output":
        options.output = readOptionValue(args, ++index, option);
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

async function readInput(input) {
  if (!input.startsWith("https://")) {
    return readFile(resolve(input), "utf8");
  }

  const response = await fetch(input);
  if (!response.ok) {
    throw new Error(`Could not download ${input}: HTTP ${response.status}`);
  }
  return response.text();
}

function compareRules(left, right) {
  return (right.priority ?? 0) - (left.priority ?? 0)
    || left.id.localeCompare(right.id);
}

function validateSnapshot(snapshot) {
  if (snapshot?.schemaVersion !== 2) {
    throw new Error("schemaVersion must be 2");
  }
  if (!snapshot.datasetVersion || !Date.parse(snapshot.generatedAt)) {
    throw new Error("datasetVersion and a valid generatedAt timestamp are required");
  }
  if (!Array.isArray(snapshot.rules) || snapshot.rules.length === 0) {
    throw new Error("The snapshot must contain at least one rule");
  }

  const ruleIds = new Set();
  for (const rule of snapshot.rules) {
    validateRule(rule, ruleIds);
  }
}

function validateRule(rule, ruleIds) {
  if (!rule || typeof rule !== "object" || Array.isArray(rule)) {
    throw new Error("Snapshot contains a rule that is not an object");
  }

  for (const field of ["id", "decisionKey", "scope"]) {
    if (!rule[field]) {
      throw new Error(`Rule is missing ${field}`);
    }
  }

  if (ruleIds.has(rule.id)) {
    throw new Error(`Duplicate rule id: ${rule.id}`);
  }
  ruleIds.add(rule.id);

  if (!rule.output?.code || !rule.output?.category || !rule.output?.status) {
    throw new Error(`Rule ${rule.id} has an incomplete output`);
  }
  if (!Array.isArray(rule.output.sources) || rule.output.sources.length === 0) {
    throw new Error(`Rule ${rule.id} must cite at least one source`);
  }
  if (rule.output.keyFacts !== undefined && (!Array.isArray(rule.output.keyFacts)
      || rule.output.keyFacts.length > 6
      || rule.output.keyFacts.some(fact => !fact?.label?.trim() || !fact?.value?.trim()))) {
    throw new Error(`Rule ${rule.id} has invalid keyFacts`);
  }

  for (const source of rule.output.sources) {
    if (source.sourceType !== "GOVERNMENT") {
      throw new Error(`Rule ${rule.id} source must be classified as GOVERNMENT`);
    }
    if (!source.url?.startsWith("https://")) {
      throw new Error(`Rule ${rule.id} source must use HTTPS`);
    }
  }
}

function printUsage() {
  console.log(
    "Usage: node scripts/import-document-rules.mjs --input <file-or-https-url> "
      + "[--output <file>]",
  );
}

try {
  await main(process.argv.slice(2));
} catch (error) {
  console.error(`Document-rule import failed: ${error.message}`);
  process.exitCode = 1;
}
