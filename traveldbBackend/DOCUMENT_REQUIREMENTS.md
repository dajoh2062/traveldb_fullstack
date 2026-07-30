# Travel-document requirements

TravelDB evaluates document requirements entirely inside the backend. A journey check never calls TravelDoc, Timatic, Sherpa, a government website, or any other external service.

## Runtime architecture

The bundled `src/main/resources/data/document-rules.json` snapshot contains normalized, versioned rules and official source citations. `LocalDocumentRulesProvider` loads and validates it when Spring starts, then evaluates:

- nationality and passport issuer;
- destination and every transit country independently;
- residence and permits;
- visas the traveller already holds;
- travel purpose and age;
- travel date, rule effective dates and expiry dates;
- rule priority and decision-key overrides.

Higher-priority rules replace lower-priority alternatives in the same decision group. For example, a rule recognizing an already-held destination visa overrides an ETA rule for the same country.

Rules past their `reviewAfter` date are automatically downgraded from `REQUIRED` or `NOT_REQUIRED` to `VERIFY`. Missing coverage also returns conservative verification guidance rather than inventing a visa decision.

## Fail-closed route coverage

Document checks are attached to the first airport where the traveller reaches each immigration jurisdiction. Consecutive domestic airports are treated as one visit, and consecutive Schengen airports are treated as one Schengen visit. A connection is evaluated with entry rules when the itinerary contains an onward domestic/intra-Schengen flight or the baggage engine says the traveller must collect a checked bag there. For example, `OSL -> JFK -> BNE -> MEL` places the U.S. permission at JFK and Australian entry at BNE, not at MEL.

The automated coverage test runs every ISO passport nationality against every ISO destination country in both transit and entry positions for a standard adult tourist profile. Each position must return either a reviewed local permission decision or a location-specific `VERIFY` result. The consumer interface displays `REQUIRED`, `CONDITIONAL`, and `VERIFY` results; it never turns missing rule coverage into a no-documents message.

This exhaustive test is a safety guarantee, not a claim that the bundled snapshot knows every immigration outcome. A definitive worldwide product still needs a licensed, continuously maintained source such as IATA Timatic or an equivalent requirements API. Until that provider is integrated, unsupported or under-specified cases must remain verification-only.

## Snapshot format

Every rule has:

- a stable ID and decision key;
- entry, transit or journey scope;
- matching conditions;
- priority and effective dates;
- last-verified and review-after dates;
- structured output status, category, explanation and exceptions;
- at least one HTTPS government source.

The current seed snapshot contains officially sourced rules for commonly used electronic permissions and transit cases in the United States, United Kingdom, Canada, Australia and New Zealand. It also has an initial Schengen entry-document set for Germany, Spain, France, Italy and the Netherlands:

- EU/EEA/Swiss citizens are told to carry a valid passport or eligible national identity card;
- other nationals receive the common passport-age and validity guidance plus an explicit `VERIFY` result for their nationality-specific visa position.

The Schengen rules deliberately do not infer visa-free entry from nationality alone. Countries without a reviewed rule still return conservative `VERIFY` guidance.

## Updating data

External data access is allowed only during an explicit collection/import operation. It is not part of the application runtime.

### Controlled refresh workflow

1. Open every cited page on the responsible government's or public authority's site. Confirm the rule text, affected nationalities, destinations, effective date and exceptions. Search results and third-party summaries are not evidence.
2. Update the candidate JSON. Set each changed rule's `lastVerified` to the review date and `reviewAfter` to no more than 120 days later. Increase `datasetVersion` using `YYYY-MM-DD.N` and set `generatedAt` to the same UTC date.
3. Import into a candidate file instead of overwriting the shipped snapshot immediately:

```powershell
cd traveldbBackend
node scripts/import-document-rules.mjs --input C:\path\to\document-rules.json --output target\document-rules.candidate.json
```

4. Run the deterministic audit with an explicit review date:

```powershell
node scripts/audit-document-rules.mjs --input target\document-rules.candidate.json --as-of 2026-07-30
node --test scripts/document-rules-audit.test.mjs
```

The audit rejects stale snapshots and rules, review windows longer than 120 days, malformed dates or tokens, duplicate IDs, insecure citations and source hosts outside the reviewed public-authority allowlist. The explicit `--as-of` value makes the same candidate produce the same freshness result in local development and CI. The audit is intentionally offline: a reviewer must still open each URL to confirm that it resolves and still supports the rule. Adding a new authority domain requires an intentional update to `OFFICIAL_SOURCE_HOST_SUFFIXES` in `scripts/document-rules-audit-lib.mjs`.

5. Review the candidate diff rule by rule. In particular, confirm that a document-only rule does not suppress an unknown visa outcome; pair it with a `VERIFY` decision when visa entitlement is not encoded.
6. Import the approved candidate into `src/main/resources/data/document-rules.json`, then run both the audit and the backend tests:

```powershell
node scripts/import-document-rules.mjs --input target\document-rules.candidate.json
node scripts/audit-document-rules.mjs --as-of 2026-07-30
mvn.cmd test
```

The dates above are examples; use the actual review date. The importer also accepts an HTTPS URL for a one-time download. It rejects duplicate IDs, unsupported schemas, non-HTTPS citations and rules without a government source. Never use the URL-import option in the application runtime.

TravelDoc's public checker is not scraped. A licensed TravelDoc export can be transformed into the local snapshot when the vendor supplies an authorized data format.

## Journey request document profile

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

The response contains `REQUIRED`, `NOT_REQUIRED`, `CONDITIONAL` or `VERIFY` requirements, the affected entry/transit location, exceptions, source URLs, missing inputs, snapshot version messaging and the check timestamp. Its concise top-level list is named `documentActions` because it includes mandatory, conditional and verification items; clients must not interpret every action as a confirmed requirement.

## Accuracy rules

- Never infer that every non-citizen needs a visa.
- Never treat Schengen membership alone as a visa decision.
- Never let a passport, health or arrival-form rule imply that an unresolved visa decision is satisfied.
- Never treat missing local coverage as visa-free entry.
- Evaluate repeated transit countries as separate itinerary stops.
- Expired review dates reduce confidence automatically.
- Admission remains a decision for the relevant border authority.
- Never describe exhaustive fail-closed test coverage as exhaustive immigration-rule coverage.
