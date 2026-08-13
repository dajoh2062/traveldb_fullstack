import { useEffect, useRef } from "react";
import AppFooter from "./components/layout/AppFooter";
import AppHeader from "./components/layout/AppHeader";
import PageIntro from "./components/layout/PageIntro";
import JourneyPlanner from "./components/planner/JourneyPlanner";
import JourneyResults from "./components/results/JourneyResults";
import useCountries from "./hooks/useCountries";
import useJourneyPlanner from "./hooks/useJourneyPlanner";
import useTheme from "./hooks/useTheme";

export default function App() {
  const { theme, toggleTheme } = useTheme();
  const {
    countries,
    error: countryError,
    isLoading: isCountryLoading,
    retry: retryCountries,
  } = useCountries();
  const journey = useJourneyPlanner();
  const resultsRef = useRef(null);

  useEffect(() => {
    if (!journey.result) return;

    resultsRef.current?.focus({ preventScroll: true });
    const prefersReducedMotion = window.matchMedia?.("(prefers-reduced-motion: reduce)").matches;
    resultsRef.current?.scrollIntoView({
      behavior: prefersReducedMotion ? "auto" : "smooth",
      block: "start",
    });
  }, [journey.result]);

  return (
    <div className="app-shell">
      <AppHeader theme={theme} onToggleTheme={toggleTheme} />
      <main id="top">
        <PageIntro />
        <JourneyPlanner
          countries={countries}
          baggage={journey.baggage}
          error={journey.error}
          fieldErrors={journey.fieldErrors}
          countryError={countryError}
          isCountryLoading={isCountryLoading}
          isLoading={journey.isLoading}
          nationality={journey.nationality}
          nationalityQuery={journey.nationalityQuery}
          onAddAirport={journey.addAirport}
          onBaggageChange={journey.updateBaggage}
          onNationalityQueryChange={journey.updateNationalityQuery}
          onMoveAirport={journey.moveAirport}
          onRemoveAirport={journey.removeAirport}
          onRetryCountries={retryCountries}
          onSelectNationality={journey.selectNationality}
          onSubmit={journey.submitJourney}
          route={journey.route}
        />
        {journey.result && (
          <div className="results-anchor" ref={resultsRef} tabIndex="-1">
            <JourneyResults result={journey.result} route={journey.route} />
          </div>
        )}
      </main>
      <AppFooter />
    </div>
  );
}
