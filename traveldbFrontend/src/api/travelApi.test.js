import { afterEach, describe, expect, it, vi } from "vitest";
import {
  checkJourney,
  fetchCountries,
  searchAirports,
} from "./travelApi";

function jsonResponse(payload, { ok = true, status = 200 } = {}) {
  return {
    json: vi.fn().mockResolvedValue(payload),
    ok,
    status,
  };
}

afterEach(() => {
  vi.unstubAllGlobals();
});

describe("travel API", () => {
  it("loads countries from the shared API endpoint", async () => {
    const countries = [{ countryId: "NO", countryNameEn: "Norway" }];
    const fetchMock = vi.fn().mockResolvedValue(jsonResponse(countries));
    vi.stubGlobal("fetch", fetchMock);

    await expect(fetchCountries()).resolves.toEqual(countries);
    expect(fetchMock).toHaveBeenCalledWith("/api/countries", { signal: undefined });
  });

  it("passes pagination through to airport search", async () => {
    const result = {
      airports: [{ iataCode: "OSL", name: "Oslo Airport" }],
      total: 81,
    };
    const fetchMock = vi.fn().mockResolvedValue(jsonResponse(result));
    vi.stubGlobal("fetch", fetchMock);

    await expect(searchAirports("Oslo", { limit: 25, offset: 50 })).resolves.toEqual(result);
    expect(fetchMock).toHaveBeenCalledWith(
      "/api/airports/search?q=Oslo&offset=50&limit=25",
      { signal: undefined },
    );
  });

  it("rejects a malformed airport response", async () => {
    vi.stubGlobal("fetch", vi.fn().mockResolvedValue(jsonResponse({ total: 1 })));

    await expect(searchAirports("OSL", { limit: 25 })).rejects.toThrow(
      "Airport search returned an invalid response",
    );
  });

  it("keeps server validation details on journey errors", async () => {
    const fieldErrors = [{ field: "route[1]", message: "Unknown airport" }];
    vi.stubGlobal("fetch", vi.fn().mockResolvedValue(jsonResponse(
      { message: "Invalid journey", errors: fieldErrors },
      { ok: false, status: 400 },
    )));

    await expect(checkJourney({ route: ["OSL", "XXX"] })).rejects.toMatchObject({
      name: "JourneyApiError",
      message: "Invalid journey",
      fieldErrors,
      status: 400,
    });
  });

  it("uses the HTTP status when an error response is not readable", async () => {
    vi.stubGlobal("fetch", vi.fn().mockResolvedValue({
      json: vi.fn().mockRejectedValue(new Error("Invalid JSON")),
      ok: false,
      status: 502,
    }));

    await expect(checkJourney({ route: ["OSL", "LHR"] })).rejects.toMatchObject({
      name: "JourneyApiError",
      message: "The journey check failed (502).",
      fieldErrors: [],
      status: 502,
    });
  });
});
