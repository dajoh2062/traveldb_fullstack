import { act, renderHook } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";
import { checkJourney } from "../api/travelApi";
import useJourneyPlanner from "./useJourneyPlanner";

vi.mock("../api/travelApi", async importOriginal => ({
  ...await importOriginal(),
  checkJourney: vi.fn(),
}));

function deferred() {
  let resolve;
  const promise = new Promise(resolvePromise => {
    resolve = resolvePromise;
  });
  return { promise, resolve };
}

function submitEvent() {
  return { preventDefault: vi.fn() };
}

afterEach(() => {
  vi.clearAllMocks();
});

describe("useJourneyPlanner", () => {
  it("submits a tourism-only regular passport search", async () => {
    checkJourney.mockResolvedValue({ pickupRequired: false });

    const { result } = renderHook(() => useJourneyPlanner());
    act(() => {
      result.current.selectNationality({ countryId: "NO", countryNameEn: "Norway" });
      result.current.addAirport({ iataCode: "OSL", name: "Oslo Airport" });
      result.current.addAirport({ iataCode: "LHR", name: "Heathrow Airport" });
    });

    await act(async () => {
      await result.current.submitJourney(submitEvent());
    });

    expect(checkJourney).toHaveBeenCalledWith({
      nationalityCountryCode: "NO",
      route: ["OSL", "LHR"],
      baggage: expect.any(Object),
      documents: { travelPurpose: "TOURISM" },
    }, { signal: expect.any(AbortSignal) });
  });

  it("does not replace a newer result with a stale response", async () => {
    const staleRequest = deferred();
    const currentResult = { pickupRequired: false, request: "current" };
    checkJourney
      .mockReturnValueOnce(staleRequest.promise)
      .mockResolvedValueOnce(currentResult);

    const { result } = renderHook(() => useJourneyPlanner());
    act(() => {
      result.current.selectNationality({ countryId: "NO", countryNameEn: "Norway" });
      result.current.addAirport({ iataCode: "OSL", name: "Oslo Airport" });
      result.current.addAirport({ iataCode: "LHR", name: "Heathrow Airport" });
    });

    let staleSubmission;
    await act(async () => {
      staleSubmission = result.current.submitJourney(submitEvent());
      await Promise.resolve();
    });

    await act(async () => {
      await result.current.submitJourney(submitEvent());
    });
    expect(result.current.result).toEqual(currentResult);

    await act(async () => {
      staleRequest.resolve({ pickupRequired: true, request: "stale" });
      await staleSubmission;
    });

    expect(result.current.result).toEqual(currentResult);
  });
});
