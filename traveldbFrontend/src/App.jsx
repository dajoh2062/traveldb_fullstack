import { useEffect, useRef, useState } from "react";
import "./App.css";
import AppFooter from "./components/AppFooter";
import AppHeader from "./components/AppHeader";
import JourneyPlanner from "./components/JourneyPlanner";
import JourneyResults from "./components/JourneyResults";
import PageIntro from "./components/PageIntro";
import useCountries from "./hooks/useCountries";
import useTheme from "./hooks/useTheme";
import {
  buildJourneyRequest,
  initialBaggageProfile,
  initialDocumentProfile,
  validateJourneyForm,
} from "./utils/journeyForm";

const documentFieldKeys = new Set([
  "residenceCountryCode",
  "passportIssuingCountryCode",
  "passportExpiryDate",
  "departureDate",
  "travelPurpose",
  "travelerAge",
  "residencePermitCountryCodes",
  "visaCountryCodes",
]);

function apiFieldKey(field = "") {
  if (field === "nationalityCountryCode") return "nationality";
  if (field === "route" || field.startsWith("route[")) return "route";
  if (field.startsWith("documents.")) return field.slice("documents.".length).split("[")[0];
  return field;
}

export default function App() {
  const { theme, toggleTheme } = useTheme();
  const {
    countries,
    error: countryError,
    isLoading: isCountryLoading,
    retry: retryCountries,
  } = useCountries();
  const resultsRef = useRef(null);
  const [nationality, setNationality] = useState("");
  const [nationalityQuery, setNationalityQuery] = useState("");
  const [route, setRoute] = useState([]);
  const [result, setResult] = useState(null);
  const [documents, setDocuments] = useState(() => ({ ...initialDocumentProfile }));
  const [baggage, setBaggage] = useState(() => ({ ...initialBaggageProfile }));
  const [advancedSearch, setAdvancedSearch] = useState(false);
  const [error, setError] = useState("");
  const [fieldErrors, setFieldErrors] = useState({});
  const [isLoading, setIsLoading] = useState(false);

  useEffect(() => {
    if (!result) return;
    resultsRef.current?.focus({ preventScroll: true });
    const reduceMotion = window.matchMedia?.("(prefers-reduced-motion: reduce)").matches;
    resultsRef.current?.scrollIntoView({ behavior: reduceMotion ? "auto" : "smooth", block: "start" });
  }, [result]);

  function clearFieldError(field) {
    setFieldErrors(current => {
      if (!current[field]) return current;
      const next = { ...current };
      delete next[field];
      return next;
    });
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
    setDocuments(current => ({
      ...current,
      passportIssuingCountryCode: current.passportIssuingCountryCode || country.countryId,
      residenceCountryCode: current.residenceCountryCode || country.countryId,
    }));
    setResult(null);
    setError("");
    clearFieldError("nationality");
  }

  function addAirport(airport) {
    if (route.some(item => item.iataCode === airport.iataCode)) {
      setError(`${airport.iataCode} is already included in this route.`);
      return false;
    }
    if (route.length >= 20) {
      setError("A journey can contain up to 20 airports.");
      return false;
    }
    setRoute(currentRoute => [...currentRoute, airport]);
    setResult(null);
    setError("");
    clearFieldError("route");
    return true;
  }

  function removeAirport(code) {
    setRoute(currentRoute => currentRoute.filter(item => item.iataCode !== code));
    setResult(null);
    clearFieldError("route");
  }

  function moveAirport(code, direction) {
    setRoute(currentRoute => {
      const currentIndex = currentRoute.findIndex(item => item.iataCode === code);
      const targetIndex = currentIndex + direction;
      if (currentIndex < 0 || targetIndex < 0 || targetIndex >= currentRoute.length) return currentRoute;

      const reorderedRoute = [...currentRoute];
      [reorderedRoute[currentIndex], reorderedRoute[targetIndex]] = [
        reorderedRoute[targetIndex],
        reorderedRoute[currentIndex],
      ];
      return reorderedRoute;
    });
    setResult(null);
    setError("");
  }

  function updateDocuments(field, value) {
    setDocuments(current => ({ ...current, [field]: value }));
    setResult(null);
    setError("");
    clearFieldError(field);
  }

  function updateBaggage(field, value) {
    setBaggage(current => ({ ...current, [field]: value }));
    setResult(null);
    setError("");
  }

  function updateAdvancedSearch(enabled) {
    setAdvancedSearch(enabled);
    setResult(null);
    setError("");
    if (!enabled) {
      setFieldErrors(current => Object.fromEntries(
        Object.entries(current).filter(([field]) => !documentFieldKeys.has(field)),
      ));
    }
  }

  async function checkJourney(event) {
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

    setIsLoading(true);
    setFieldErrors({});
    const controller = new AbortController();
    const timeout = window.setTimeout(() => controller.abort(), 15000);
    try {
      const response = await fetch("/api/journey/check", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(buildJourneyRequest({
          nationality,
          route,
          baggage,
          documents,
          includeDocumentDetails: advancedSearch,
        })),
        signal: controller.signal,
      });
      const payload = await response.json().catch(() => null);
      if (!response.ok) {
        if (payload?.errors?.length) {
          setFieldErrors(Object.fromEntries(
            payload.errors.map(item => [apiFieldKey(item.field), item.message]),
          ));
        }
        throw new Error(payload?.message ?? payload?.detail ?? `The journey check failed (${response.status}).`);
      }
      if (!payload) throw new Error("The journey checker returned an unreadable response.");
      setResult(payload);
    } catch (requestError) {
      setError(
        requestError.name === "AbortError"
          ? "The check took too long. Confirm that the service is available and try again."
          : requestError.message || "We could not check this journey. Please try again.",
      );
    } finally {
      window.clearTimeout(timeout);
      setIsLoading(false);
    }
  }

  return (
    <div className="app-shell">
      <AppHeader theme={theme} onToggleTheme={toggleTheme} />
      <main id="top">
        <PageIntro />
        <JourneyPlanner
          advancedSearch={advancedSearch}
          countries={countries}
          baggage={baggage}
          documents={documents}
          error={error}
          fieldErrors={fieldErrors}
          countryError={countryError}
          isCountryLoading={isCountryLoading}
          isLoading={isLoading}
          nationality={nationality}
          nationalityQuery={nationalityQuery}
          onAddAirport={addAirport}
          onAdvancedSearchChange={updateAdvancedSearch}
          onBaggageChange={updateBaggage}
          onDocumentChange={updateDocuments}
          onNationalityQueryChange={updateNationalityQuery}
          onMoveAirport={moveAirport}
          onRemoveAirport={removeAirport}
          onRetryCountries={retryCountries}
          onSelectNationality={selectNationality}
          onSubmit={checkJourney}
          route={route}
        />
        {result && (
          <div className="results-anchor" ref={resultsRef} tabIndex="-1">
            <JourneyResults
              result={result}
              route={route}
            />
          </div>
        )}
      </main>
      <AppFooter />
    </div>
  );
}
