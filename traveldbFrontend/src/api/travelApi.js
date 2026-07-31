async function readJson(response) {
  return response.json().catch(() => null);
}

export class JourneyApiError extends Error {
  constructor(message, { fieldErrors = [], status } = {}) {
    super(message);
    this.name = "JourneyApiError";
    this.fieldErrors = fieldErrors;
    this.status = status;
  }
}

export async function fetchCountries({ signal } = {}) {
  const response = await fetch("/api/countries", { signal });
  if (!response.ok) throw new Error("Country service unavailable");

  const countries = await readJson(response);
  if (!Array.isArray(countries)) throw new Error("Country service returned an invalid response");
  return countries;
}

export async function searchAirports(query, { limit = 50, offset = 0, signal } = {}) {
  const params = new URLSearchParams({
    q: query,
    offset: String(offset),
    limit: String(limit),
  });
  const response = await fetch(`/api/airports/search?${params}`, { signal });
  if (!response.ok) throw new Error("Airport search unavailable");

  const payload = await readJson(response);
  if (Array.isArray(payload)) {
    return { airports: payload, total: payload.length };
  }
  if (!payload || !Array.isArray(payload.airports)) {
    throw new Error("Airport search returned an invalid response");
  }

  return {
    airports: payload.airports,
    total: payload.total ?? payload.airports.length,
  };
}

export async function checkJourney(request, { signal } = {}) {
  const response = await fetch("/api/journey/check", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(request),
    signal,
  });
  const payload = await readJson(response);

  if (!response.ok) {
    throw new JourneyApiError(
      payload?.message ?? payload?.detail ?? `The journey check failed (${response.status}).`,
      {
        fieldErrors: payload?.errors ?? [],
        status: response.status,
      },
    );
  }
  if (!payload) throw new Error("The journey checker returned an unreadable response.");

  return payload;
}
