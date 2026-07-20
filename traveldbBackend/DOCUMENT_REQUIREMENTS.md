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

## Snapshot format

Every rule has:

- a stable ID and decision key;
- entry, transit or journey scope;
- matching conditions;
- priority and effective dates;
- last-verified and review-after dates;
- structured output status, category, explanation and exceptions;
- at least one HTTPS government source.

The current seed snapshot contains officially sourced rules for commonly used electronic permissions and transit cases in the United States, United Kingdom, Canada, Australia and New Zealand. Other countries are still handled locally, but return `VERIFY` until a reviewed country rule is imported.

## Updating data

External data access is allowed only during an explicit collection/import operation. It is not part of the application runtime.

After converting a licensed TravelDoc/Timatic export or government-source collection to the normalized snapshot schema, import it with:

```powershell
cd traveldbBackend
node scripts/import-document-rules.mjs --input C:\path\to\document-rules.json
mvn.cmd test
```

The importer also accepts an HTTPS URL for a one-time download. It rejects duplicate IDs, unsupported schemas, non-HTTPS citations and rules without a government source. Review generated changes before committing them.

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

The response contains `REQUIRED`, `NOT_REQUIRED`, `CONDITIONAL` or `VERIFY` requirements, the affected entry/transit location, exceptions, source URLs, missing inputs, snapshot version messaging and the check timestamp.

## Accuracy rules

- Never infer that every non-citizen needs a visa.
- Never treat Schengen membership alone as a visa decision.
- Never treat missing local coverage as visa-free entry.
- Evaluate repeated transit countries as separate itinerary stops.
- Expired review dates reduce confidence automatically.
- Admission remains a decision for the relevant border authority.
