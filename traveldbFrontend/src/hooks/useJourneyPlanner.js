import { useEffect, useRef, useState } from "react";
import { useTranslation } from "react-i18next";
import { checkJourney, JourneyApiError } from "../api/travelApi";
import {
  buildJourneyRequest,
  initialBaggageProfile,
  mapApiErrorsToFields,
  validateJourneyForm,
} from "../utils/journeyForm";
import { countryDisplayName } from "../utils/search";

const MAX_AIRPORTS_PER_ROUTE = 20;
const JOURNEY_CHECK_TIMEOUT_MS = 15_000;

export default function useJourneyPlanner() {
  const { i18n, t } = useTranslation();
  const activeRequestRef = useRef(null);
  const [nationality, setNationality] = useState("");
  const [nationalityQuery, setNationalityQuery] = useState("");
  const [selectedCountry, setSelectedCountry] = useState(null);
  const [route, setRoute] = useState([]);
  const [baggage, setBaggage] = useState(() => ({ ...initialBaggageProfile }));
  const [result, setResult] = useState(null);
  const [error, setError] = useState("");
  const [fieldErrors, setFieldErrors] = useState({});
  const [isLoading, setIsLoading] = useState(false);

  useEffect(() => () => activeRequestRef.current?.abort(), []);

  useEffect(() => {
    if (selectedCountry) {
      setNationalityQuery(countryDisplayName(selectedCountry, i18n.resolvedLanguage));
    }
  }, [i18n.resolvedLanguage, selectedCountry]);

  function clearFieldError(field) {
    setFieldErrors(currentErrors => {
      if (!currentErrors[field]) return currentErrors;

      const nextErrors = { ...currentErrors };
      delete nextErrors[field];
      return nextErrors;
    });
  }

  function clearResultAndError() {
    setResult(null);
    setError("");
  }

  function updateNationalityQuery(value) {
    setNationalityQuery(value);
    setNationality("");
    setSelectedCountry(null);
    setResult(null);
    clearFieldError("nationality");
  }

  function selectNationality(country) {
    setNationality(country.countryId);
    setNationalityQuery(countryDisplayName(country, i18n.resolvedLanguage));
    setSelectedCountry(country);
    clearResultAndError();
    clearFieldError("nationality");
  }

  function addAirport(airport) {
    if (route.some(routeAirport => routeAirport.iataCode === airport.iataCode)) {
      setError(t("validation.duplicateAirport", { code: airport.iataCode }));
      return false;
    }
    if (route.length >= MAX_AIRPORTS_PER_ROUTE) {
      setError(t("validation.routeMaximum", { count: MAX_AIRPORTS_PER_ROUTE }));
      return false;
    }

    setRoute(currentRoute => [...currentRoute, airport]);
    clearResultAndError();
    clearFieldError("route");
    return true;
  }

  function removeAirport(code) {
    setRoute(currentRoute => currentRoute.filter(airport => airport.iataCode !== code));
    setResult(null);
    clearFieldError("route");
  }

  function moveAirport(code, direction) {
    setRoute(currentRoute => {
      const currentIndex = currentRoute.findIndex(airport => airport.iataCode === code);
      const targetIndex = currentIndex + direction;
      if (currentIndex < 0 || targetIndex < 0 || targetIndex >= currentRoute.length) {
        return currentRoute;
      }

      const reorderedRoute = [...currentRoute];
      [reorderedRoute[currentIndex], reorderedRoute[targetIndex]] = [
        reorderedRoute[targetIndex],
        reorderedRoute[currentIndex],
      ];
      return reorderedRoute;
    });
    clearResultAndError();
  }

  function updateBaggage(field, value) {
    setBaggage(currentBaggage => ({ ...currentBaggage, [field]: value }));
    clearResultAndError();
  }

  async function submitJourney(event) {
    event.preventDefault();
    setError("");
    setResult(null);

    const validationErrors = validateJourneyForm({ nationality, route }, t);
    if (Object.keys(validationErrors).length > 0) {
      setFieldErrors(validationErrors);
      setError(t("validation.reviewHighlighted"));
      window.requestAnimationFrame(() => document.getElementById("journey-form-error")?.focus());
      return;
    }

    const request = buildJourneyRequest({ nationality, route, baggage });
    activeRequestRef.current?.abort();
    const controller = new AbortController();
    activeRequestRef.current = controller;
    const timeout = window.setTimeout(() => controller.abort(), JOURNEY_CHECK_TIMEOUT_MS);

    setIsLoading(true);
    setFieldErrors({});
    try {
      const journeyResult = await checkJourney(request, { signal: controller.signal });
      if (activeRequestRef.current !== controller) return;
      setResult(journeyResult);
    } catch (requestError) {
      if (activeRequestRef.current !== controller) return;
      if (requestError instanceof JourneyApiError && requestError.fieldErrors.length > 0) {
        setFieldErrors(mapApiErrorsToFields(requestError.fieldErrors, t));
      }
      setError(
        requestError.name === "AbortError"
          ? t("errors.journeyTimeout")
          : t("errors.journeyFallback"),
      );
    } finally {
      window.clearTimeout(timeout);
      if (activeRequestRef.current === controller) {
        activeRequestRef.current = null;
        setIsLoading(false);
      }
    }
  }

  return {
    baggage,
    error,
    fieldErrors,
    isLoading,
    nationality,
    nationalityQuery,
    result,
    route,
    addAirport,
    moveAirport,
    removeAirport,
    selectNationality,
    submitJourney,
    updateBaggage,
    updateNationalityQuery,
  };
}
