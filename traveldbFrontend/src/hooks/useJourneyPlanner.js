import { useEffect, useRef, useState } from "react";
import { checkJourney, JourneyApiError } from "../api/travelApi";
import {
  buildJourneyRequest,
  createInitialDocumentProfile,
  initialBaggageProfile,
  mapApiErrorsToFields,
  removeDocumentFieldErrors,
  validateJourneyForm,
} from "../utils/journeyForm";
import { isPassportLikeDocument } from "../utils/travelDocuments";

const MAX_AIRPORTS_PER_ROUTE = 20;
const JOURNEY_CHECK_TIMEOUT_MS = 15_000;

export default function useJourneyPlanner() {
  const activeRequestRef = useRef(null);
  const [nationality, setNationality] = useState("");
  const [nationalityQuery, setNationalityQuery] = useState("");
  const [route, setRoute] = useState([]);
  const [documents, setDocuments] = useState(createInitialDocumentProfile);
  const [baggage, setBaggage] = useState(() => ({ ...initialBaggageProfile }));
  const [advancedSearch, setAdvancedSearch] = useState(false);
  const [result, setResult] = useState(null);
  const [error, setError] = useState("");
  const [fieldErrors, setFieldErrors] = useState({});
  const [isLoading, setIsLoading] = useState(false);

  useEffect(() => () => activeRequestRef.current?.abort(), []);

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
    setResult(null);
    clearFieldError("nationality");
  }

  function selectNationality(country) {
    setNationality(country.countryId);
    setNationalityQuery(country.countryNameEn);
    setDocuments(currentDocuments => ({
      ...currentDocuments,
      residenceCountryCode: currentDocuments.residenceCountryCode || country.countryId,
      travelDocuments: currentDocuments.travelDocuments.map(document => (
        document.primary
        && isPassportLikeDocument(document.type)
        && !document.issuingCountryCode
          ? { ...document, issuingCountryCode: country.countryId }
          : document
      )),
    }));
    clearResultAndError();
    clearFieldError("nationality");
    clearFieldError("travelDocuments");
  }

  function addAirport(airport) {
    if (route.some(routeAirport => routeAirport.iataCode === airport.iataCode)) {
      setError(`${airport.iataCode} is already included in this route.`);
      return false;
    }
    if (route.length >= MAX_AIRPORTS_PER_ROUTE) {
      setError(`A journey can contain up to ${MAX_AIRPORTS_PER_ROUTE} airports.`);
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

  function updateDocuments(field, value) {
    setDocuments(currentDocuments => ({ ...currentDocuments, [field]: value }));
    clearResultAndError();
    clearFieldError(field);
    if (field === "departureDate") clearFieldError("travelDocuments");
  }

  function updateBaggage(field, value) {
    setBaggage(currentBaggage => ({ ...currentBaggage, [field]: value }));
    clearResultAndError();
  }

  function updateAdvancedSearch(enabled) {
    setAdvancedSearch(enabled);
    clearResultAndError();

    if (!enabled) {
      setFieldErrors(removeDocumentFieldErrors);
    }
  }

  async function submitJourney(event) {
    event.preventDefault();
    setError("");
    setResult(null);

    const validationErrors = validateJourneyForm({
      nationality,
      route,
      documents,
      includeDocumentDetails: advancedSearch,
    });
    if (Object.keys(validationErrors).length > 0) {
      setFieldErrors(validationErrors);
      setError("Please review the highlighted details before checking this journey.");
      window.requestAnimationFrame(() => document.getElementById("journey-form-error")?.focus());
      return;
    }

    const request = buildJourneyRequest({
      nationality,
      route,
      baggage,
      documents,
      includeDocumentDetails: advancedSearch,
    });
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
        setFieldErrors(mapApiErrorsToFields(requestError.fieldErrors, documents));
      }
      setError(
        requestError.name === "AbortError"
          ? "The check took too long. Confirm that the service is available and try again."
          : requestError.message || "We could not check this journey. Please try again.",
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
    advancedSearch,
    baggage,
    documents,
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
    updateAdvancedSearch,
    updateBaggage,
    updateDocuments,
    updateNationalityQuery,
  };
}
