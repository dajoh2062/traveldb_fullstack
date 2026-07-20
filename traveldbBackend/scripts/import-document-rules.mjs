import { readFile, writeFile } from "node:fs/promises";
import { resolve } from "node:path";
import process from "node:process";

const args = new Map();
for (let index = 2; index < process.argv.length; index += 2) {
  args.set(process.argv[index], process.argv[index + 1]);
}

const input = args.get("--input");
const output = resolve(
  args.get("--output") ?? "src/main/resources/data/document-rules.json",
);

if (!input) {
  throw new Error("Usage: node scripts/import-document-rules.mjs --input <file-or-https-url> [--output <file>]");
}

const raw = input.startsWith("https://")
  ? await fetch(input).then(async response => {
      if (!response.ok) throw new Error(`Download failed with HTTP ${response.status}`);
      return response.text();
    })
  : await readFile(resolve(input), "utf8");

const snapshot = JSON.parse(raw);
validateSnapshot(snapshot);

snapshot.rules.sort((left, right) =>
  (right.priority ?? 0) - (left.priority ?? 0) || left.id.localeCompare(right.id),
);

await writeFile(output, `${JSON.stringify(snapshot, null, 2)}\n`, "utf8");
console.log(`Imported ${snapshot.rules.length} document rules from ${input}`);
console.log(`Dataset ${snapshot.datasetVersion} written to ${output}`);

function validateSnapshot(value) {
  if (value?.schemaVersion !== 1) throw new Error("schemaVersion must be 1");
  if (!value.datasetVersion || !Date.parse(value.generatedAt)) {
    throw new Error("datasetVersion and a valid generatedAt timestamp are required");
  }
  if (!Array.isArray(value.rules) || value.rules.length === 0) {
    throw new Error("The snapshot must contain at least one rule");
  }

  const ids = new Set();
  for (const rule of value.rules) {
    for (const field of ["id", "decisionKey", "scope"]) {
      if (!rule[field]) throw new Error(`Rule is missing ${field}`);
    }
    if (ids.has(rule.id)) throw new Error(`Duplicate rule id: ${rule.id}`);
    ids.add(rule.id);
    if (!rule.output?.code || !rule.output?.category || !rule.output?.status) {
      throw new Error(`Rule ${rule.id} has an incomplete output`);
    }
    if (!Array.isArray(rule.output.sources) || rule.output.sources.length === 0) {
      throw new Error(`Rule ${rule.id} must cite at least one source`);
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
}
