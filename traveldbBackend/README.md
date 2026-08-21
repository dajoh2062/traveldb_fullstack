# TravelDB backend

Spring Boot API for airport search, baggage transfer guidance, and ordinary-passport checks.

## Development

Run the application from this directory:

```powershell
.\mvnw.cmd spring-boot:run
```

Run the backend test suite with:

```powershell
.\mvnw.cmd test
```

On macOS or Linux, use `./mvnw` instead of `.\mvnw.cmd`.

## Source layout

```text
src/main/java/io/github/dajoh2062/traveldb/
  api/          HTTP endpoints, DTOs, filters, and error responses
  baggage/      Checked-baggage transfer rules
  config/       Application configuration shared across services
  documents/    Passport rule loading and evaluation
  model/        Airport and country records
  repository/   Database access
  service/      Journey orchestration and search services
```

The HTTP layer is split by resource (`JourneyController`, `AirportController`, `CountryController`, and `HealthController`). Controllers delegate use cases to services, and services use JDBC repositories for persisted data.

## Persistence

Flyway owns the database schema in `src/main/resources/db/migration`. The default local database is in-memory H2; production should use PostgreSQL through these environment variables:

```text
TRAVELDB_DATABASE_URL=jdbc:postgresql://host:5432/traveldb
TRAVELDB_DATABASE_USERNAME=traveldb
TRAVELDB_DATABASE_PASSWORD=...
```

`TRAVELDB_DATABASE_URL` accepts both JDBC URLs and provider-style `postgresql://` URLs. The checked-in Render Blueprint provisions and wires a free PostgreSQL instance; choose a paid database plan before relying on it for production durability or availability.

On an empty database, the application loads the bundled country and airport reference data. Set `TRAVELDB_REFERENCE_DATA_BOOTSTRAP_ENABLED=false` when those tables are managed separately.

The reviewed `document-rules.json` artifact is validated and imported into versioned relational tables. One dataset is activated atomically. A deployment containing a newer dataset version imports and activates it; redeploying the same version is idempotent. Set `TRAVELDB_DOCUMENT_RULES_BOOTSTRAP_ENABLED=false` when rule publication is handled by a separate administrative process.

Baggage policies are also database-backed. Flyway seeds a versioned dataset containing ordered rule selectors, airport groups, output text, exceptions, sources, and its reviewed date. `BaggageRuleRepository` loads the active dataset and `BaggageRuleMatcher` evaluates it without database access inside the matching algorithm.

H2 remains useful for local development and tests, but production data survives deployments only when a persistent PostgreSQL datasource is configured.

## Maintenance

- [Document rule format and review process](docs/document-rules.md)
- [Baggage transfer rules and sources](docs/baggage-rules.md)
- [Data maintenance scripts](scripts/README.md)

The API is documented in the [root README](../README.md#api).
