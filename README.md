# TravelDB

TravelDB is a journey checker for baggage reclaim and travel-document requirements. The React app collects an itinerary and traveller profile; the Spring Boot API evaluates both against data bundled with the repository.

Requests do not call an immigration or airport-data service at runtime. Results are guidance, not a substitute for instructions from an airline or border authority.

## Run it locally

You need Java 21 and Node.js 20.19 or newer.

Start the backend in one PowerShell terminal:

```powershell
cd traveldbBackend
.\mvnw.cmd spring-boot:run
```

Start the frontend in another:

```powershell
cd traveldbFrontend
npm ci
npm run dev
```

Open <http://localhost:5173>. Vite forwards `/api` requests to the backend at `http://localhost:8080`.

On macOS or Linux, use `./mvnw` in place of `.\mvnw.cmd`.

## Repository map

| Path | What lives there |
| --- | --- |
| [`traveldbFrontend/src`](traveldbFrontend/src) | React pages, components, hooks, form validation, and API calls |
| [`traveldbBackend/src/main/java/projects/traveldbbackend/api`](traveldbBackend/src/main/java/projects/traveldbbackend/api) | REST endpoints, API errors, and request/response DTOs |
| [`traveldbBackend/src/main/java/projects/traveldbbackend/service`](traveldbBackend/src/main/java/projects/traveldbbackend/service) | Request validation and journey orchestration |
| [`traveldbBackend/src/main/java/projects/traveldbbackend/model`](traveldbBackend/src/main/java/projects/traveldbbackend/model) | Airport and country domain models |
| [`traveldbBackend/src/main/java/projects/traveldbbackend/repository`](traveldbBackend/src/main/java/projects/traveldbbackend/repository) | H2 queries and result mapping |
| [`traveldbBackend/src/main/java/projects/traveldbbackend/rules`](traveldbBackend/src/main/java/projects/traveldbbackend/rules) | Baggage rule engine and structured advice |
| [`traveldbBackend/src/main/java/projects/traveldbbackend/documents`](traveldbBackend/src/main/java/projects/traveldbbackend/documents) | Travel-document visit resolution and snapshot evaluation |
| [`traveldbBackend/src/main/resources/data`](traveldbBackend/src/main/resources/data) | Generated airport/country SQL and document rules |
| [`traveldbBackend/src/test`](traveldbBackend/src/test) | Backend unit, integration, validation, and rule-coverage tests |
| [`traveldbBackend/scripts`](traveldbBackend/scripts) | Offline data import, generation, and audit tools |

More detailed guides:

- [Frontend development guide](traveldbFrontend/README.md)
- [Baggage rule behavior and sources](traveldbBackend/BAGGAGE_RULES.md)
- [Travel-document snapshot and review workflow](traveldbBackend/DOCUMENT_REQUIREMENTS.md)
- [Backend data scripts](traveldbBackend/scripts/README.md)

## Request flow

```text
React UI
  -> GET /api/countries and /api/airports/search
  -> POST /api/journey/check
       -> TravelController
       -> TravelService
            -> JourneyRequestValidator + TravelRepository -> in-memory H2 data
            -> RuleEngine -> baggage advice
            -> DocumentRequirementsProvider -> LocalDocumentRulesProvider
                                              -> DocumentRuleSnapshotLoader
                                              -> bundled document-rules.json
       <- one journey response with baggage stops and document actions
```

H2 is recreated from the SQL seed files each time the backend starts. The document-rule JSON snapshot is loaded and validated during startup.

## API endpoints

| Method | Endpoint | Purpose |
| --- | --- | --- |
| `GET` | `/api/countries` | List countries used by the traveller form |
| `GET` | `/api/airports/search?q=osl&offset=0&limit=50` | Search airports by code, name, or location |
| `POST` | `/api/journey/check` | Evaluate baggage handling and document actions for an itinerary |

See [the document-rule guide](traveldbBackend/DOCUMENT_REQUIREMENTS.md#journey-request-profile) for an example journey payload.

## Verification

Backend checks, run from `traveldbBackend`:

```powershell
node --test scripts/document-rules-audit.test.mjs
node scripts/audit-document-rules.mjs --as-of 2026-07-30
.\mvnw.cmd test
```

The bundled snapshot was reviewed on `2026-07-30`. When refreshing it, replace that value with the actual source-review date. An explicit date keeps freshness checks reproducible.

Frontend checks, run from `traveldbFrontend`:

```powershell
npm ci
npm run check
```

`npm run check` runs the frontend tests, ESLint, and the production build.

## Updating reference data

- Follow [the controlled document-rule workflow](traveldbBackend/DOCUMENT_REQUIREMENTS.md#refreshing-the-snapshot) before changing immigration guidance.
- Run `node scripts/sync-ourairports-data.mjs` from `traveldbBackend` to refresh the public-domain [OurAirports](https://ourairports.com/data/) snapshot. The command rewrites generated country and airport files, so review their metadata and diff before committing.

Current source URLs, generation time, filters, and record counts are recorded in [`ourairports-metadata.json`](traveldbBackend/src/main/resources/data/ourairports-metadata.json).
