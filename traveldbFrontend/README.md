# TravelDB frontend

React client for checking ordinary-passport and checked-baggage requirements for an itinerary.

## Development

Start the backend first, then run:

```bash
npm ci
npm run dev
```

Vite serves the app at `http://localhost:5173` and proxies `/api` to
`http://localhost:8080`.

Before committing frontend changes, run:

```bash
npm run check
```

Use `npm run test:watch` while developing and `npm run format` to apply the project
formatting rules.

## Project structure

```text
src/
  api/          HTTP client functions
  components/   UI grouped by layout, planner, results, and shared controls
  hooks/        React state and side effects
  i18n/         Language metadata, detection, and translation resources
  styles/       Global, layout, planner, result, and responsive styles
  test/         Shared test setup
  utils/        Pure form, search, and display helpers
```

Tests live next to the files they cover.

## Languages

The interface uses i18next, remembers the selected language, and falls back to British English.
Country names, dates, lists, validation, and accessibility labels follow the active locale. Arabic
also switches the document to right-to-left layout.

Interface catalogs are plain JSON files in `src/i18n/locales`. Passport and baggage guidance lives
in `src/i18n/guidance` and is selected through stable rule and advice identifiers returned by the API.

The API keeps its original English wording for compatibility. The client uses that wording as a
clearly marked fallback when it receives a newer dataset or an identifier that its catalogs do not
yet cover. Official source names are left unchanged.

## Search scope

The form supports tourism journeys made with a regular (ordinary) passport. It does
not collect passport details or support diplomatic, service, official, military, or
other specialist travel documents.

The interface from before the August 2026 redesign is retained in Git at commit
`e769e07` and on the local backup branch `codex/ui-backup-before-refresh-2026-08-13`.
