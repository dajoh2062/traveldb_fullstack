import { useState } from "react";
import "./App.css";
import AppFooter from "./components/AppFooter";
import AppHeader from "./components/AppHeader";
import JourneyPlanner from "./components/JourneyPlanner";
import JourneyResults from "./components/JourneyResults";
import PageIntro from "./components/PageIntro";
import useCountries from "./hooks/useCountries";
import useTheme from "./hooks/useTheme";

export default function App() {
  const { theme, toggleTheme } = useTheme();
  const { countries, isLoading: isCountryLoading } = useCountries();
  const [nationality, setNationality] = useState("NO");
  const [nationalityQuery, setNationalityQuery] = useState("Norway");
  const [route, setRoute] = useState([]);
  const [result, setResult] = useState(null);
  const [baggage, setBaggage] = useState({
    checkedBaggage: true,
    ticketArrangement: "UNKNOWN",
    checkedThrough: "UNKNOWN",
  });
  const [error, setError] = useState("");
  const [isLoading, setIsLoading] = useState(false);

  function updateNationalityQuery(value) {
    setNationalityQuery(value);
    setNationality("");
    setResult(null);
  }

  function selectNationality(country) {
    setNationality(country.countryId);
    setNationalityQuery(country.countryNameEn);
    setResult(null);
    setError("");
  }

  function addAirport(airport) {
    if (route.some(item => item.iataCode === airport.iataCode)) return false;
    setRoute(currentRoute => [...currentRoute, airport]);
    setResult(null);
    setError("");
    return true;
  }

  function removeAirport(code) {
    setRoute(currentRoute => currentRoute.filter(item => item.iataCode !== code));
    setResult(null);
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

  function updateBaggage(field, value) {
    setBaggage(current => ({ ...current, [field]: value }));
    setResult(null);
    setError("");
  }

  async function checkJourney(event) {
    event.preventDefault();
    setError("");
    setResult(null);

    if (!/^[A-Za-z]{2}$/.test(nationality.trim())) {
      setError("Select a passport nationality from the search results.");
      return;
    }
    if (route.length < 2) {
      setError("Add at least an origin and a destination.");
      return;
    }

    setIsLoading(true);
    try {
      const response = await fetch("/api/journey/check", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          nationalityCountryCode: nationality.trim().toUpperCase(),
          route: route.map(airport => airport.iataCode),
          baggage,
        }),
      });
      if (!response.ok) throw new Error(`The journey check failed (${response.status}).`);
      setResult(await response.json());
    } catch (requestError) {
      setError(requestError.message || "We could not check this journey. Please try again.");
    } finally {
      setIsLoading(false);
    }
  }

  return (
    <div className="app-shell">
      <AppHeader theme={theme} onToggleTheme={toggleTheme} />
      <main id="top">
        <PageIntro />
        <JourneyPlanner
          countries={countries}
          baggage={baggage}
          error={error}
          isCountryLoading={isCountryLoading}
          isLoading={isLoading}
          nationality={nationality}
          nationalityQuery={nationalityQuery}
          onAddAirport={addAirport}
          onBaggageChange={updateBaggage}
          onNationalityQueryChange={updateNationalityQuery}
          onMoveAirport={moveAirport}
          onRemoveAirport={removeAirport}
          onSelectNationality={selectNationality}
          onSubmit={checkJourney}
          route={route}
        />
        {result && (
          <JourneyResults
            nationality={nationality}
            nationalityQuery={nationalityQuery}
            baggage={baggage}
            result={result}
            route={route}
          />
        )}
      </main>
      <AppFooter />
    </div>
  );
}
