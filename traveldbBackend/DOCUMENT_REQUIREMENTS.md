# Travel-document rules

TravelDB evaluates travel-document rules from a versioned JSON snapshot bundled with the backend. Journey requests never call TravelDoc, Timatic, Sherpa, or a government website.

## Runtime design

| Component | Responsibility |
| --- | --- |
| `DocumentRouteVisitResolver` | Finds the first airport at which each immigration jurisdiction is reached. |
| `TravelService` | Builds the traveller and route context, including baggage collection points. |
| `DocumentRequirementsProvider` | Defines the provider boundary used by journey checks. |
| `DocumentRuleSnapshotLoader` | Parses and validates `document-rules.json` during startup. |
| `LocalDocumentRulesProvider` | Matches the loaded rules and merges location-specific fallback guidance. |
| `ConservativeDocumentProvider` | Produces location-specific `VERIFY` guidance when local coverage is missing. |

The resolver treats consecutive domestic airports as one country visit and consecutive Schengen airports as one Schengen visit. A transit point is evaluated with entry rules when the itinerary continues domestically or within Schengen, or when the baggage engine requires the traveller to collect a checked bag there.

For example, `OSL -> JFK -> BNE -> MEL` places U.S. permission checks at JFK and Australian entry checks at BNE, not MEL.

Within a visit, higher-priority rules replace lower-priority rules with the same `decisionKey`. This lets a rule for an already-held destination visa override a general ETA rule. A rule past its `reviewAfter` date is downgraded from `REQUIRED` or `NOT_REQUIRED` to `VERIFY` at runtime.

## Snapshot contract

The bundled snapshot is [`src/main/resources/data/document-rules.json`](src/main/resources/data/document-rules.json). It contains:

- `schemaVersion`, currently `1`;
- a `datasetVersion` in `YYYY-MM-DD.N` form and a matching UTC `generatedAt` timestamp;
- snapshot-level official sources; and
- a list of rules.

Each rule has a stable ID and decision key, a journey/entry/transit scope, matching conditions, priority, effective dates, review dates, structured output, and at least one HTTPS government source.

Current reviewed coverage includes common electronic permissions and transit cases for the United States, United Kingdom, Canada, Australia, and New Zealand. Germany, Spain, France, Italy, and the Netherlands also have an initial Schengen entry-document set. Those rules cover eligible EU/EEA/Swiss identity documents and common passport-validity constraints, but do not infer nationality-specific visa-free entry.

The coverage test checks every ISO passport nationality against every ISO destination in transit and entry positions for a standard adult tourist profile. Every case must return either a reviewed decision or a location-specific `VERIFY` result. This is fail-closed route coverage, not a claim that the snapshot contains every immigration rule.

## Refreshing the snapshot

External access is allowed only as part of this explicit maintenance workflow.

1. Open every cited government or public-authority page. Confirm the rule, affected travellers, effective date, and exceptions. Search results and third-party summaries are not evidence.
2. Update the candidate JSON. Set changed rules' `lastVerified` dates, keep `reviewAfter` within 120 days, increment `datasetVersion`, and make the version date match `generatedAt`.
3. Import to a candidate file:

   ```powershell
   cd traveldbBackend
   node scripts/import-document-rules.mjs --input C:\path\to\document-rules.json --output target\document-rules.candidate.json
   ```

4. Audit and test the candidate. Replace the example audit date with the actual review date:

   ```powershell
   node scripts/audit-document-rules.mjs --input target\document-rules.candidate.json --as-of 2026-07-30
   node --test scripts/document-rules-audit.test.mjs
   ```

5. Review the diff rule by rule. A document-only rule must not suppress an unknown visa outcome; add a `VERIFY` decision when visa entitlement is not encoded.
6. Import the approved file into the bundled snapshot, then run the full backend suite:

   ```powershell
   node scripts/import-document-rules.mjs --input target\document-rules.candidate.json
   node scripts/audit-document-rules.mjs --as-of 2026-07-30
   .\mvnw.cmd test
   ```

The audit is offline and deterministic. It rejects stale data, review windows longer than 120 days, malformed dates or tokens, duplicate rule IDs, insecure citations, and source hosts outside `OFFICIAL_SOURCE_HOST_SUFFIXES` in `scripts/document-rules-audit-lib.mjs`. It cannot confirm that a URL still resolves or that its page still supports the rule; the reviewer must do that.

The importer also accepts an HTTPS URL for a one-time download. Do not use URL import in application code. TravelDoc's public checker is not scraped; a licensed export can only be used when the vendor provides an authorized data format.

## Journey request profile

```json
{
  "nationalityCountryCode": "NO",
  "route": ["OSL", "LHR"],
  "documents": {
    "residenceCountryCode": "NO",
    "passportIssuingCountryCode": "NO",
    "passportExpiryDate": "2029-05-10",
    "departureDate": "2026-09-14",
    "travelPurpose": "TOURISM",
    "travelerAge": 30,
    "residencePermitCountryCodes": [],
    "visaCountryCodes": []
  }
}
```

The response includes `REQUIRED`, `NOT_REQUIRED`, `CONDITIONAL`, and `VERIFY` requirements, their entry or transit locations, exceptions, sources, missing inputs, snapshot version, and check time. The short top-level `documentActions` list includes mandatory, conditional, and verification items; clients must not treat every item as a confirmed requirement.

## Safety invariants

- Missing local coverage must never become a visa-free result.
- Schengen membership alone is not a visa decision.
- Passport, health, or arrival-form rules cannot resolve an unknown visa decision.
- Repeated visits to a country are evaluated as separate itinerary stops.
- Expired review dates reduce confidence automatically.
- Final admission decisions belong to the relevant border authority.
