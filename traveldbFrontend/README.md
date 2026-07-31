# TravelDB frontend

React and Vite client for building an itinerary and checking baggage and document requirements.

## Local development

```bash
npm ci
npm run dev
```

The dev server runs at `http://localhost:5173` and proxies `/api` requests to the backend at `http://localhost:8080`.

Run the complete frontend verification before opening a pull request:

```bash
npm run check
```

You can also run the test suite in watch mode with `npm run test:watch`.

## Source layout

- `src/api` contains HTTP requests and response error handling.
- `src/components` contains the rendered interface. Components used only by the travel-document form are grouped in `components/documents`.
- `src/hooks` owns reusable state and side effects, including journey form orchestration and remote search state.
- `src/utils` contains pure validation, request-building, and display helpers.
- Tests live beside the module or component they cover. Shared test setup is in `src/test`.

Keep network details out of components and put form rules in `utils/journeyForm.js` so they can be tested without rendering the UI.

## Advanced document checks

Advanced search accepts multiple passports, identity documents, permits, visas, and specialist travel documents. One document is marked as primary for the journey, while visas and residence permits are kept as separate entries. The form intentionally does not collect document numbers.

The request builder sends each entry as `documents.travelDocuments` with its type, optional custom name, issuing country, optional expiry date, and primary flag. It also derives the older passport, visa, and residence-permit fields required by backend compatibility. Do not add separate form state for those compatibility fields.
