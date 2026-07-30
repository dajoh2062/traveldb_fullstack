import { useEffect, useRef, useState } from "react";
import { normalizeSearch } from "../utils/search";

const airportSearchCache = new Map();
const PAGE_SIZE = 50;

function unpackSearchResponse(payload) {
  if (Array.isArray(payload)) return { airports: payload, total: payload.length };
  return { airports: payload.airports ?? [], total: payload.total ?? 0 };
}

export default function useAirportSearch() {
  const queryRef = useRef("");
  const [query, setQuery] = useState("");
  const [suggestions, setSuggestions] = useState([]);
  const [isOpen, setIsOpen] = useState(false);
  const [isSearching, setIsSearching] = useState(false);
  const [isLoadingMore, setIsLoadingMore] = useState(false);
  const [searchError, setSearchError] = useState("");
  const [total, setTotal] = useState(0);

  useEffect(() => {
    const trimmedQuery = query.trim();
    if (trimmedQuery.length < 1) return undefined;

    const cacheKey = normalizeSearch(trimmedQuery);
    if (airportSearchCache.has(cacheKey)) return undefined;

    const controller = new AbortController();
    const timer = window.setTimeout(() => {
      fetch(`/api/airports/search?q=${encodeURIComponent(trimmedQuery)}&offset=0&limit=${PAGE_SIZE}`, { signal: controller.signal })
        .then(response => response.ok ? response.json() : Promise.reject(new Error("Airport search unavailable")))
        .then(payload => {
          const searchResult = unpackSearchResponse(payload);
          airportSearchCache.set(cacheKey, searchResult);
          setSuggestions(searchResult.airports);
          setTotal(searchResult.total);
        })
        .catch(() => {
          if (!controller.signal.aborted) {
            setSuggestions([]);
            setTotal(0);
            setSearchError("Airport search is temporarily unavailable.");
          }
        })
        .finally(() => { if (!controller.signal.aborted) setIsSearching(false); });
    }, /^[a-z]{3}$/i.test(trimmedQuery) ? 0 : 35);

    return () => {
      window.clearTimeout(timer);
      controller.abort();
    };
  }, [query]);

  function updateQuery(value) {
    const cacheKey = normalizeSearch(value);
    const cachedResult = airportSearchCache.get(cacheKey);
    queryRef.current = value;
    setQuery(value);
    setSuggestions(cachedResult?.airports ?? []);
    setTotal(cachedResult?.total ?? 0);
    setIsSearching(Boolean(value.trim()) && !cachedResult);
    setIsLoadingMore(false);
    setSearchError("");
    setIsOpen(true);
  }

  async function loadMore() {
    if (isLoadingMore || suggestions.length >= total) return;

    const requestedQuery = query.trim();
    const cacheKey = normalizeSearch(requestedQuery);
    const offset = suggestions.length;
    setIsLoadingMore(true);

    try {
      const response = await fetch(
        `/api/airports/search?q=${encodeURIComponent(requestedQuery)}&offset=${offset}&limit=${PAGE_SIZE}`
      );
      if (!response.ok) throw new Error("Airport search failed");
      const nextPage = unpackSearchResponse(await response.json());
      if (normalizeSearch(queryRef.current) !== cacheKey) return;

      setSuggestions(currentSuggestions => {
        const knownCodes = new Set(currentSuggestions.map(airport => airport.iataCode));
        const combined = [
          ...currentSuggestions,
          ...nextPage.airports.filter(airport => !knownCodes.has(airport.iataCode)),
        ];
        airportSearchCache.set(cacheKey, { airports: combined, total: nextPage.total });
        return combined;
      });
      setTotal(nextPage.total);
    } catch {
      // Keep the already loaded results available if a later page fails.
      setSearchError("More airports could not be loaded. Try again.");
    } finally {
      setIsLoadingMore(false);
    }
  }

  function clearSearch() {
    queryRef.current = "";
    setQuery("");
    setSuggestions([]);
    setIsSearching(false);
    setIsLoadingMore(false);
    setTotal(0);
    setSearchError("");
    setIsOpen(false);
  }

  return {
    clearSearch,
    hasMore: suggestions.length < total,
    isLoadingMore,
    isOpen,
    isSearching,
    loadMore,
    query,
    searchError,
    setIsOpen,
    suggestions,
    total,
    updateQuery,
  };
}
