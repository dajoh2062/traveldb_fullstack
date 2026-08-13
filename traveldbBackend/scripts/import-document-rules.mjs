import { mkdir, readFile, writeFile } from "node:fs/promises";
import { dirname, resolve } from "node:path";
import process from "node:process";
import { readOptionValue, utcDate } from "./cli-options.mjs";
import { auditDocumentRulesSnapshot } from "./document-rules-audit-lib.mjs";

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
  const audit = auditDocumentRulesSnapshot(snapshot, {
    asOf: options.asOf ?? utcDate(),
  });
  if (audit.errors.length > 0) {
    throw new Error(`Snapshot failed validation:\n- ${audit.errors.join("\n- ")}`);
  }

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
    asOf: null,
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
      case "--as-of":
        options.asOf = readOptionValue(args, ++index, option);
        break;
      default:
        throw new Error(`Unknown option: ${option}`);
    }
  }

  return options;
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

function printUsage() {
  console.log(
    "Usage: node scripts/import-document-rules.mjs --input <file-or-https-url> "
      + "[--output <file>] [--as-of YYYY-MM-DD]",
  );
}

try {
  await main(process.argv.slice(2));
} catch (error) {
  console.error(`Document-rule import failed: ${error.message}`);
  process.exitCode = 1;
}
