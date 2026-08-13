import { act, fireEvent, render, screen } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";
import { searchAirports } from "../../api/travelApi";
import AirportSearch from "./AirportSearch";

vi.mock("../../api/travelApi", () => ({
  searchAirports: vi.fn(),
}));

const oslo = {
  city: "Oslo",
  country: "Norway",
  countryCode: "NO",
  iataCode: "OSL",
  name: "Oslo Airport",
};

const london = {
  city: "London",
  country: "United Kingdom",
  countryCode: "GB",
  iataCode: "LHR",
  name: "London Heathrow Airport",
};

const paris = {
  city: "Paris",
  country: "France",
  countryCode: "FR",
  iataCode: "CDG",
  name: "Charles de Gaulle Airport",
};

function deferred() {
  let resolve;
  let reject;
  const promise = new Promise((resolvePromise, rejectPromise) => {
    resolve = resolvePromise;
    reject = rejectPromise;
  });
  return { promise, reject, resolve };
}

async function finishSearchDelay() {
  await act(async () => {
    await vi.runAllTimersAsync();
  });
}

afterEach(() => {
  vi.clearAllMocks();
  vi.useRealTimers();
});

describe("AirportSearch", () => {
  it("shows the destination country flag for airport suggestions", async () => {
    vi.useFakeTimers();
    searchAirports.mockResolvedValue({ airports: [oslo], total: 1 });
    render(<AirportSearch onSelect={vi.fn()} />);

    fireEvent.change(screen.getByRole("combobox", { name: "Add an airport" }), {
      target: { value: "OSL" },
    });
    await finishSearchDelay();

    expect(document.querySelector(".country-code-cell .country-flag")).toHaveAttribute(
      "data-country-code",
      "no",
    );
  });

  it("keeps arrow-key navigation and selection working", async () => {
    vi.useFakeTimers();
    searchAirports.mockResolvedValue({ airports: [oslo, london], total: 2 });
    const onSelect = vi.fn();
    render(<AirportSearch onSelect={onSelect} />);

    const input = screen.getByRole("combobox", { name: "Add an airport" });
    fireEvent.change(input, { target: { value: "keyboard-route-test" } });
    await finishSearchDelay();

    fireEvent.keyDown(input, { key: "ArrowDown" });
    fireEvent.keyDown(input, { key: "Enter" });

    expect(onSelect).toHaveBeenCalledWith(london);
    expect(input).toHaveValue("");
  });

  it("aborts the previous request when the query changes", () => {
    vi.useFakeTimers();
    searchAirports.mockImplementation(() => new Promise(() => {}));
    render(<AirportSearch onSelect={vi.fn()} />);

    const input = screen.getByRole("combobox", { name: "Add an airport" });
    fireEvent.change(input, { target: { value: "ABC" } });
    act(() => vi.runOnlyPendingTimers());

    const firstSignal = searchAirports.mock.calls[0][1].signal;
    expect(firstSignal.aborted).toBe(false);

    fireEvent.change(input, { target: { value: "replacement-query" } });

    expect(firstSignal.aborted).toBe(true);
  });

  it("loads the next page after the current result count", async () => {
    vi.useFakeTimers();
    searchAirports
      .mockResolvedValueOnce({ airports: [oslo], total: 2 })
      .mockResolvedValueOnce({ airports: [london], total: 2 });
    render(<AirportSearch onSelect={vi.fn()} />);

    fireEvent.change(screen.getByRole("combobox", { name: "Add an airport" }), {
      target: { value: "pagination-route-test" },
    });
    await finishSearchDelay();
    await act(async () => {
      fireEvent.click(screen.getByRole("button", { name: "Show more" }));
    });

    expect(searchAirports).toHaveBeenLastCalledWith(
      "pagination-route-test",
      expect.objectContaining({
        limit: 50,
        offset: 1,
      }),
    );
    expect(screen.getByRole("option", { name: /London Heathrow Airport/ })).toBeInTheDocument();
  });

  it("ignores a stale page rejection after the query changes", async () => {
    vi.useFakeTimers();
    const stalePage = deferred();
    const currentPage = deferred();
    searchAirports
      .mockResolvedValueOnce({ airports: [oslo], total: 2 })
      .mockReturnValueOnce(stalePage.promise)
      .mockResolvedValueOnce({ airports: [london], total: 2 })
      .mockReturnValueOnce(currentPage.promise);
    render(<AirportSearch onSelect={vi.fn()} />);

    const input = screen.getByRole("combobox", { name: "Add an airport" });
    fireEvent.change(input, { target: { value: "stale-pagination-query" } });
    await finishSearchDelay();
    fireEvent.click(screen.getByRole("button", { name: "Show more" }));

    const staleSignal = searchAirports.mock.calls[1][1].signal;
    fireEvent.change(input, { target: { value: "current-pagination-query" } });
    expect(staleSignal.aborted).toBe(true);
    await finishSearchDelay();
    fireEvent.click(screen.getByRole("button", { name: "Show more" }));

    const loadingButton = screen.getByRole("button", { name: /Loading/ });
    expect(loadingButton).toBeDisabled();
    await act(async () => {
      stalePage.reject(new Error("stale failure"));
      await Promise.resolve();
    });

    expect(
      screen.queryByText("More airports could not be loaded. Try again."),
    ).not.toBeInTheDocument();
    expect(loadingButton).toBeDisabled();

    await act(async () => {
      currentPage.resolve({ airports: [paris], total: 2 });
      await Promise.resolve();
    });
    expect(screen.getByRole("option", { name: /Charles de Gaulle Airport/ })).toBeInTheDocument();
  });
});
