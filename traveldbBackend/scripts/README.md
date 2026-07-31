# Backend data scripts

Run these commands from `traveldbBackend`. They use Node's standard library and do not require a separate npm install.

| Command | Network | Writes files | Purpose |
| --- | --- | --- | --- |
| `node scripts/audit-document-rules.mjs --as-of YYYY-MM-DD` | No | No | Validate schema, sources, review windows, and freshness. |
| `node --test scripts/document-rules-audit.test.mjs` | No | No | Run regression tests for the audit library. |
| `node scripts/import-document-rules.mjs --input <file-or-https-url>` | Only for an HTTPS input | Yes | Validate, sort, and write a document-rule snapshot. |
| `node scripts/sync-ourairports-data.mjs` | Yes | Yes | Download OurAirports CSV files and regenerate airport/country seeds. |

Use `--help` with the audit or import command for its full options.

## Document rules

Do not import unreviewed rules directly into `src/main/resources/data/document-rules.json`. Write to `target/document-rules.candidate.json`, run the audit with the review date, inspect the diff, and only then replace the bundled snapshot. The complete checklist is in [`../DOCUMENT_REQUIREMENTS.md`](../DOCUMENT_REQUIREMENTS.md#refreshing-the-snapshot).

The importer creates the output directory when needed. Rules are written in priority order, with rule ID as the stable tie-breaker.

## Airport and country data

`sync-ourairports-data.mjs` rewrites:

- `src/main/resources/data/countries.sql`;
- `src/main/resources/data/airports.sql`; and
- `src/main/resources/data/ourairports-metadata.json`.

Duplicate IATA codes are resolved deterministically by scheduled-service status, airport type, ICAO-code availability, and source ID. Review generated counts and diffs, then run `.\mvnw.cmd test`.
