# TravelDB

TravelDB checks a flight itinerary for checked-baggage transfer steps and passport requirements. The React frontend sends the route to a Spring Boot API, which evaluates it against reference data stored in this repository.

The web form is intended for tourist trips using a regular (ordinary) passport. It does not support diplomatic, service, official, military, refugee, or other specialist travel documents. Results are guidance; always confirm them with the airline and the relevant border authorities.

## Requirements

- Java 21
- Node.js 24

## Run locally

Start the backend:

```powershell
cd traveldbBackend
.\mvnw.cmd spring-boot:run
```

In another terminal, start the frontend:

```powershell
cd traveldbFrontend
npm ci
npm run dev
```

Open <http://localhost:5173>. The Vite development server forwards `/api` requests to `http://localhost:8080`.

On macOS or Linux, use `./mvnw` instead of `.\mvnw.cmd`.

## Checks

Run the backend checks from `traveldbBackend`. Replace `YYYY-MM-DD` with the date being audited.

```powershell
node --test scripts/document-rules-audit.test.mjs
node scripts/audit-document-rules.mjs --as-of YYYY-MM-DD
.\mvnw.cmd test
```

Run the frontend checks from `traveldbFrontend`:

```powershell
npm ci
npm run check
```

`npm run check` verifies formatting, runs the tests and ESLint, and creates a production build. GitHub Actions runs the backend and frontend checks on pull requests and pushes to `main`.

## Project layout

- [`traveldbFrontend`](traveldbFrontend) contains the React application. See its [development guide](traveldbFrontend/README.md).
- [`traveldbBackend`](traveldbBackend) contains the API, journey services, bundled data, tests, and maintenance scripts. See its [development guide](traveldbBackend/README.md).

Flyway manages the backend schema. Local development defaults to in-memory H2, while the Render Blueprint wires the API to PostgreSQL. Country and airport reference data bootstrap empty databases, the reviewed document-rule artifact is imported into versioned relational tables, and baggage policies are seeded as an independently versioned active dataset. Journey checks do not fetch immigration or airport data at runtime.

## API

| Method | Endpoint | Purpose |
| --- | --- | --- |
| `GET` | `/api/countries` | List countries used by the traveller form |
| `GET` | `/api/airports/search?q=osl&offset=0&limit=50` | Search airports by code, name, or location |
| `POST` | `/api/journey/check` | Check baggage handling and travel-document requirements |
| `GET` | `/api/health` | Health check |

## Maintaining reference data

- Follow the [document-rule workflow](traveldbBackend/docs/document-rules.md#refreshing-the-snapshot) before changing passport or immigration guidance.
- Read the [baggage rule guide](traveldbBackend/docs/baggage-rules.md) before changing baggage-transfer logic or source links.
- Run `node scripts/sync-ourairports-data.mjs` from `traveldbBackend` to refresh the [OurAirports](https://ourairports.com/data/) snapshot. The current source URLs, filters, generation time, and record counts are stored in [`ourairports-metadata.json`](traveldbBackend/src/main/resources/data/ourairports-metadata.json).
- See the [scripts guide](traveldbBackend/scripts/README.md) for import and audit commands.
