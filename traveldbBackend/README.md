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

Airport and country data is loaded into an in-memory H2 database from `src/main/resources/data`. Passport rules are read from the bundled `document-rules.json` snapshot.

## Maintenance

- [Document rule format and review process](docs/document-rules.md)
- [Baggage transfer rules and sources](docs/baggage-rules.md)
- [Data maintenance scripts](scripts/README.md)

The API is documented in the [root README](../README.md#api).
