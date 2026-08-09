import assert from "node:assert/strict";
import test from "node:test";
import { auditDocumentRulesSnapshot } from "./document-rules-audit-lib.mjs";

test("accepts a fresh snapshot with a reviewed government source", () => {
  const audit = auditDocumentRulesSnapshot(snapshot(), { asOf: "2026-07-30" });

  assert.deepEqual(audit.errors, []);
  assert.equal(audit.summary.ruleCount, 1);
  assert.equal(audit.summary.nextReviewAfter, "2026-10-30");
});

test("accepts the reviewed Brazilian and Norwegian authority hosts", () => {
  const value = snapshot();
  const sources = [
    {
      label: "Brazilian Ministry of Foreign Affairs",
      url: "https://www.gov.br/mre/pt-br/assuntos/portal-consular/qgrv-simples-port-18jul25.pdf",
      sourceType: "GOVERNMENT",
    },
    {
      label: "Norwegian Ministry of Foreign Affairs",
      url: "https://www.regjeringen.no/no/tema/utenrikssaker/reiseinformasjon/velg-land/reiseinfo_brasil/id2415169/",
      sourceType: "GOVERNMENT",
    },
    {
      label: "Brazilian immigration decree",
      url: "https://www.planalto.gov.br/ccivil_03/_ato2015-2018/2017/decreto/d9199.htm",
      sourceType: "GOVERNMENT",
    },
  ];
  value.sources = sources;
  value.rules[0].output.sources = sources;

  const audit = auditDocumentRulesSnapshot(value, { asOf: "2026-07-30" });

  assert.deepEqual(audit.errors, []);
});

test("accepts the reviewed Japanese authority hosts", () => {
  const value = snapshot();
  const sources = [
    {
      label: "Japan Ministry of Foreign Affairs",
      url: "https://www.mofa.go.jp/j_info/visit/visa/short/novisa.html",
      sourceType: "GOVERNMENT",
    },
    {
      label: "Embassy of Japan in Finland",
      url: "https://www.fi.emb-japan.go.jp/itpr_fi/consular.html",
      sourceType: "GOVERNMENT",
    },
    {
      label: "Immigration Services Agency of Japan",
      url: "https://www.moj.go.jp/isa/immigration/procedures/kikoku_00001.html",
      sourceType: "GOVERNMENT",
    },
  ];
  value.sources = sources;
  value.rules[0].output.sources = sources;

  const audit = auditDocumentRulesSnapshot(value, { asOf: "2026-07-30" });

  assert.deepEqual(audit.errors, []);
});

test("reports stale rules and unreviewed source hosts deterministically", () => {
  const value = snapshot();
  value.rules[0].reviewAfter = "2026-07-01";
  value.rules[0].output.sources[0].url = "https://travel-rules.example/entry";

  const audit = auditDocumentRulesSnapshot(value, { asOf: "2026-07-30" });

  assert.ok(audit.errors.includes("rule test-entry is past its review date (2026-07-01)"));
  assert.ok(audit.errors.includes(
    "rule test-entry.output.sources[0].url host is not in the reviewed authority allowlist: travel-rules.example",
  ));
});

test("rejects a verification date after the explicit audit date", () => {
  const value = snapshot();
  value.rules[0].lastVerified = "2026-07-31";

  const audit = auditDocumentRulesSnapshot(value, { asOf: "2026-07-30" });

  assert.ok(audit.errors.includes("rule test-entry lastVerified is after audit date 2026-07-30"));
});

test("rejects malformed or duplicate key facts", () => {
  const value = snapshot();
  value.rules[0].output.keyFacts = [
    { label: "Maximum stay", value: "Up to 90 days" },
    { label: "maximum stay", value: "" },
  ];

  const audit = auditDocumentRulesSnapshot(value, { asOf: "2026-07-30" });

  assert.ok(audit.errors.includes("rule test-entry.output.keyFacts[1].value must be non-empty text"));
  assert.ok(audit.errors.includes(
    "rule test-entry.output.keyFacts contains duplicate label: maximum stay",
  ));
});

function snapshot() {
  const source = {
    label: "European Union authority",
    url: "https://europa.eu/youreurope/example",
    sourceType: "GOVERNMENT",
  };
  return {
    schemaVersion: 2,
    datasetVersion: "2026-07-30.1",
    generatedAt: "2026-07-30T12:00:00Z",
    sources: [source],
    rules: [{
      id: "test-entry",
      decisionKey: "TEST_ENTRY",
      scope: "ENTRY",
      destinationCountries: ["DE"],
      nationalities: ["NO"],
      priority: 100,
      effectiveFrom: "2026-01-01",
      lastVerified: "2026-07-30",
      reviewAfter: "2026-10-30",
      output: {
        code: "TEST_DOCUMENT",
        category: "TRAVEL_DOCUMENT",
        status: "REQUIRED",
        title: "Travel document",
        summary: "A travel document is required.",
        conditions: [],
        keyFacts: [{ label: "Accepted document", value: "Valid passport" }],
        sources: [{ ...source }],
      },
    }],
  };
}
