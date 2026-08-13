import { useEffect, useRef, useState } from "react";
import { useTranslation } from "react-i18next";
import { searchAirports } from "../api/travelApi";
import { normalizeSearch } from "../utils/search";

const airportSearchCache = new Map();
const CACHE_CAPACITY = 100;
const PAGE_SIZE = 50;
const SEARCH_DEBOUNCE_MS = 200;

function readCachedSearch(cacheKey) {
  const cachedResult = airportSearchCache.get(cacheKey);
  if (!cachedResult) return undefined;

  // Refresh the entry so the least recently used searches expire first.
  airportSearchCache.delete(cacheKey);
  airportSearchCache.set(cacheKey, cachedResult);
  return cachedResult;
}

function cacheSearch(cacheKey, searchResult) {
  airportSearchCache.delete(cacheKey);
  airportSearchCache.set(cacheKey, searchResult);

  if (airportSearchCache.size > CACHE_CAPACITY) {
    const oldestKey = airportSearchCache.keys().next().value;
    airportSearchCache.delete(oldestKey);
  }
}

export default function useAirportSearch() {
  const { t } = useTranslation();
  const queryRef = useRef("");
  const loadMoreRequestRef = useRef(null);
  const [query, setQuery] = useState("");
  const [suggestions, setSuggestions] = useState([]);
  const [isOpen, setIsOpen] = useState(false);
  const [isSearching, setIsSearching] = useState(false);
  const [isLoadingMore, setIsLoadingMore] = useState(false);
  const [searchErrorKey, setSearchErrorKey] = useState("");
  const [total, setTotal] = useState(0);

  useEffect(() => {
    const trimmedQuery = query.trim();
    if (trimmedQuery.length < 1) return undefined;

    const cacheKey = normalizeSearch(trimmedQuery);
    if (airportSearchCache.has(cacheKey)) return undefined;

    const controller = new AbortController();
    const timer = window.setTimeout(
      () => {
        searchAirports(trimmedQuery, { limit: PAGE_SIZE, signal: controller.signal })
          .then(searchResult => {
            cacheSearch(cacheKey, searchResult);
            if (controller.signal.aborted || normalizeSearch(queryRef.current) !== cacheKey) return;
            setSuggestions(searchResult.airports);
            setTotal(searchResult.total);
          })
          .catch(() => {
            if (!controller.signal.aborted) {
              setSuggestions([]);
              setTotal(0);
              setSearchErrorKey("errors.airportSearch");
            }
          })
          .finally(() => {
            if (!controller.signal.aborted) setIsSearching(false);
          });
      },
      /^[a-z]{3}$/i.test(trimmedQuery) ? 0 : SEARCH_DEBOUNCE_MS,
    );

    return () => {
      window.clearTimeout(timer);
      controller.abort();
    };
  }, [query]);

  function updateQuery(value) {
    loadMoreRequestRef.current?.controller.abort();
    loadMoreRequestRef.current = null;

    const cacheKey = normalizeSearch(value);
    const cachedResult = readCachedSearch(cacheKey);
    queryRef.current = value;
    setQuery(value);
    setSuggestions(cachedResult?.airports ?? []);
    setTotal(cachedResult?.total ?? 0);
    setIsSearching(Boolean(value.trim()) && !cachedResult);
    setIsLoadingMore(false);
    setSearchErrorKey("");
    setIsOpen(true);
  }

  async function loadMore() {
    if (isLoadingMore || suggestions.length >= total) return;

    const requestedQuery = query.trim();
    const cacheKey = normalizeSearch(requestedQuery);
    const offset = suggestions.length;
    const request = {
      cacheKey,
      controller: new AbortController(),
    };
    loadMoreRequestRef.current = request;
    setIsLoadingMore(true);

    try {
      const nextPage = await searchAirports(requestedQuery, {
        limit: PAGE_SIZE,
        offset,
        signal: request.controller.signal,
      });
      if (
        loadMoreRequestRef.current !== request ||
        request.controller.signal.aborted ||
        normalizeSearch(queryRef.current) !== request.cacheKey
      )
        return;

      setSuggestions(currentSuggestions => {
        const knownCodes = new Set(currentSuggestions.map(airport => airport.iataCode));
        const combined = [
          ...currentSuggestions,
          ...nextPage.airports.filter(airport => !knownCodes.has(airport.iataCode)),
        ];
        cacheSearch(request.cacheKey, { airports: combined, total: nextPage.total });
        return combined;
      });
      setTotal(nextPage.total);
    } catch {
      if (
        loadMoreRequestRef.current !== request ||
        request.controller.signal.aborted ||
        normalizeSearch(queryRef.current) !== request.cacheKey
      )
        return;

      // Keep the already loaded results available if a later page fails.
      setSearchErrorKey("errors.airportPage");
    } finally {
      if (loadMoreRequestRef.current === request) {
        loadMoreRequestRef.current = null;
        setIsLoadingMore(false);
      }
    }
  }

  function clearSearch() {
    loadMoreRequestRef.current?.controller.abort();
    loadMoreRequestRef.current = null;
    queryRef.current = "";
    setQuery("");
    setSuggestions([]);
    setIsSearching(false);
    setIsLoadingMore(false);
    setTotal(0);
    setSearchErrorKey("");
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
    searchError: searchErrorKey ? t(searchErrorKey) : "",
    setIsOpen,
    suggestions,
    updateQuery,
  };
}
