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
- `src/components` contains the rendered interface. Components used only by the passport form are grouped in `components/documents`.
- `src/hooks` owns reusable state and side effects, including journey form orchestration and remote search state.
- `src/utils` contains pure validation, request-building, and display helpers.
- Tests live beside the module or component they cover. Shared test setup is in `src/test`.

Keep network details out of components and put form rules in `utils/journeyForm.js` so they can be tested without rendering the UI.
