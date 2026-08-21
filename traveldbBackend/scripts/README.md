# Backend data scripts

Run these commands from `traveldbBackend`. They use Node's standard library, so there is no separate package installation.

| Command | Network access | Writes files | Purpose |
| --- | --- | --- | --- |
| `node scripts/audit-document-rules.mjs --as-of YYYY-MM-DD` | No | No | Check the snapshot schema, sources, review windows, and freshness. |
| `node --test scripts/document-rules-audit.test.mjs` | No | No | Run regression tests for the audit library. |
| `node scripts/import-document-rules.mjs --input <file-or-https-url>` | Only for an HTTPS input | Yes | Validate, sort, and write a document-rule snapshot. |
| `node scripts/sync-ourairports-data.mjs` | Yes | Yes | Download OurAirports data and rebuild the airport and country seeds. |

The audit and import commands support `--help`.

## Document rules

Import changes to `target/document-rules.candidate.json` first. Audit the candidate, inspect its diff, and only then replace `src/main/resources/data/document-rules.json`. See the [document rule guide](../docs/document-rules.md#refreshing-the-snapshot) for the full review procedure.

The bundled snapshot is an import artifact. On application startup, a new reviewed dataset version is written to the database and atomically made active; journey requests read the active database dataset through `DocumentRuleRepository`.

The importer creates its output directory when necessary and writes rules in priority order, using the rule ID as a stable tie-breaker.

## Airport and country data

`sync-ourairports-data.mjs` rewrites:

- `src/main/resources/data/countries.sql`
- `src/main/resources/data/airports.sql`
- `src/main/resources/data/ourairports-metadata.json`

Duplicate IATA codes are resolved by scheduled-service status, airport type, ICAO-code availability, and source ID. Review the generated counts and diff, then run `.\mvnw.cmd test`.
