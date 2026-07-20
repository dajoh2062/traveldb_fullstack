# TravelDB

TravelDB checks baggage-reclaim points and travel-document requirements for a passenger's complete airport itinerary.

## Global reference data

The backend bundles a deterministic snapshot of the public-domain [OurAirports](https://ourairports.com/data/) datasets. Runtime behavior does not depend on a third-party API.

Current coverage:

- 249 countries and country-like ISO entities
- 9,056 non-closed airports with three-letter IATA codes
- IATA and ICAO codes
- Airport and municipality names
- Country, ISO region, and continent
- Scheduled-service and airport-type indicators
- Latitude, longitude, and elevation
- Official airport and Wikipedia links when available
- Alternate names and search keywords
- Current Schengen membership stored separately from the aviation source

The generated snapshot metadata is stored in `traveldbBackend/src/main/resources/data/ourairports-metadata.json`.

### Refreshing the dataset

From `traveldbBackend` run:

```powershell
node scripts/sync-ourairports-data.mjs
```

The command downloads the current `airports.csv` and `countries.csv` snapshots, resolves duplicate IATA codes deterministically, and regenerates the bundled SQL files. Review and test generated changes before committing them.

OurAirports data is released into the public domain and carries no guarantee of accuracy or fitness for use. Immigration, document, and baggage rules must come from authoritative government and airline sources; airport reference data alone cannot determine them.

## Travel-document checking

Journey checks use a versioned rule snapshot evaluated entirely by the backend; no third-party API is called at runtime. External sources are used only by an explicit data-import workflow. See [traveldbBackend/DOCUMENT_REQUIREMENTS.md](traveldbBackend/DOCUMENT_REQUIREMENTS.md) for the rule schema, source policy, request fields and update process.

## Development

Backend:

```powershell
cd traveldbBackend
.\mvnw.cmd spring-boot:run
```

Frontend:

```powershell
cd traveldbFrontend
npm install
npm run dev
```

Frontend: `http://localhost:5173`
Backend: `http://localhost:8080`

Run verification:

```powershell
cd traveldbBackend
mvn.cmd test

cd ..\traveldbFrontend
npm.cmd run lint
npm.cmd run build
```
