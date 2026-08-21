# Travel-document rules

TravelDB evaluates a reviewed, versioned rule dataset stored in the application database. The bundled JSON snapshot is the validated import artifact used to publish a new dataset during deployment. Journey checks do not call government websites or third-party immigration services at runtime.

The browser client is limited to tourist trips using a regular (ordinary) passport. It sends nationality, route, baggage details, and `travelPurpose: TOURISM`; it does not collect passport numbers or other sensitive document identifiers.

## Runtime behavior

`DocumentRouteVisitResolver` determines where each immigration jurisdiction is first reached. Consecutive domestic airports count as one country visit, and consecutive Schengen airports count as one Schengen visit. A connection is treated as an entry point when the itinerary continues domestically or within Schengen, or when the traveller must collect checked baggage there.

`DocumentRuleRepository` loads the active dataset from relational tables. `LocalDocumentRulesProvider` caches that immutable dataset and matches the visit and traveller profile against its rules. Within a visit, a higher-priority rule replaces a lower-priority rule with the same `decisionKey`. A rule past its `reviewAfter` date is returned as `VERIFY` instead of a definitive `REQUIRED` or `NOT_REQUIRED` result.

If no local rule covers a visit, `ConservativeDocumentProvider` returns location-specific verification guidance. The bootstrap importer accepts only classpath and local-file snapshot locations; HTTP and HTTPS locations are rejected during startup.

Flyway creates the dataset, rule, selector, condition, source, and key-fact tables. Dataset activation is represented by a single database row and is changed in the same transaction as an import. Rules are never updated in place: a new reviewed snapshot creates a new dataset version, and the previous dataset remains available for audit history.

## Snapshot format

The bundled snapshot is [`src/main/resources/data/document-rules.json`](../src/main/resources/data/document-rules.json). It contains:

- a `schemaVersion`;
- a dated `datasetVersion` and matching UTC `generatedAt` timestamp;
- snapshot-level official sources; and
- rules with stable IDs, matching conditions, priority, effective dates, review dates, structured output, and HTTPS sources.

A rule can apply to the whole journey, an entry, or a transit. Results use four statuses:

| Status | Meaning |
| --- | --- |
| `REQUIRED` | The stored rule says an action or document is required. |
| `NOT_REQUIRED` | The stored rule says it is not required for the supplied profile. |
| `CONDITIONAL` | The answer depends on details that the stored rule cannot fully resolve. |
| `VERIFY` | The traveller must check current official guidance. |

Coverage changes with each snapshot and is not described as a fixed percentage. Uncovered routes must return `VERIFY`, never an inferred visa-free result.

## Refreshing the snapshot

Rule changes require a source review. Search results and third-party summaries are not sufficient evidence.

1. Open each cited government or public-authority page and confirm the rule, affected travellers, effective date, and exceptions.
2. Update a candidate JSON file. Change `lastVerified` for reviewed rules, keep `reviewAfter` within 120 days, increment `datasetVersion`, and make its date match `generatedAt`.
3. Import the candidate into `target`:

   ```powershell
   cd traveldbBackend
   node scripts/import-document-rules.mjs --input C:\path\to\document-rules.json --output target\document-rules.candidate.json
   ```

4. Audit the candidate and run the audit tests. Replace `YYYY-MM-DD` with the review date.

   ```powershell
   node scripts/audit-document-rules.mjs --input target\document-rules.candidate.json --as-of YYYY-MM-DD
   node --test scripts/document-rules-audit.test.mjs
   ```

5. Review the diff rule by rule. A passport, health, or arrival-form rule must not hide an unknown visa outcome.
6. Import the approved candidate and run the full backend suite:

   ```powershell
   node scripts/import-document-rules.mjs --input target\document-rules.candidate.json
   node scripts/audit-document-rules.mjs --as-of YYYY-MM-DD
   .\mvnw.cmd test
   ```

7. Deploy the backend. At startup, the validated bundled version is inserted into the database and atomically activated if it differs from the active version. An unchanged version is not inserted again.

The audit checks dates, review windows, IDs, source URLs, and the official-source host allowlist in `scripts/document-rules-audit-lib.mjs`. It cannot confirm that a source page still supports a rule; that remains a manual review step.

The importer can download an HTTPS input for a one-time maintenance import. Application code must continue to use the bundled snapshot.

## Journey contract

The web client sends the ordinary-passport profile in this form:

```json
{
  "nationalityCountryCode": "NO",
  "route": ["OSL", "LHR"],
  "baggage": {
    "checkedBaggage": true,
    "ticketArrangement": "SINGLE_BOOKING",
    "checkedThrough": "YES"
  },
  "documents": {
    "travelPurpose": "TOURISM"
  }
}
```

The response provides a short `documentActions` list and detailed `documentCheck.requirements`. Detailed requirements include their status, entry or transit location, conditions, key facts, source links, and review dates. The response also includes missing inputs, warnings, the dataset version, and the check time. `NOT_REQUIRED` results remain in the detailed list but are not included in `documentActions`.

## Safety invariants

- Missing coverage must never become a visa-free result.
- Schengen membership alone is not a visa decision.
- Passport, health, and arrival-form rules cannot resolve an unknown visa decision.
- Nationality-based passport waivers apply only to an ordinary passport. National identity cards match only the reviewed EU, EEA, and Swiss free-movement rule.
- Repeated visits to the same country are evaluated separately.
- Expired review dates reduce a rule to verification guidance.
- Final admission decisions belong to the relevant border authority.
