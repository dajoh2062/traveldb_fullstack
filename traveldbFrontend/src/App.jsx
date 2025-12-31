import { useEffect, useState } from "react";
import "./App.css";

export default function App() {
  const [theme, setTheme] = useState("light");
  const [nationality, setNationality] = useState("NO");

  const [query, setQuery] = useState("");
  const [suggestions, setSuggestions] = useState([]);
  const [route, setRoute] = useState([]);

  const [result, setResult] = useState(null);
  const [error, setError] = useState("");

  useEffect(() => {
    document.documentElement.setAttribute("data-theme", theme);
  }, [theme]);

  useEffect(() => {
    if (query.length < 2) {
      setSuggestions([]);
      return;
    }

    fetch(`/api/airports/search?q=${encodeURIComponent(query)}`)
      .then(r => r.json())
      .then(setSuggestions)
      .catch(() => setSuggestions([]));
  }, [query]);

  function addAirport(airport) {
    if (route.some(a => a.iataCode === airport.iataCode)) return;
    setRoute([...route, airport]);
    setQuery("");
    setSuggestions([]);
  }

  function removeAirport(code) {
    setRoute(route.filter(a => a.iataCode !== code));
  }

  async function submit(e) {
    e.preventDefault();
    setError("");
    setResult(null);

    if (route.length < 2) {
      setError("Add at least two airports");
      return;
    }

    try {
      const res = await fetch("/api/journey/check", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          nationalityCountryCode: nationality.toUpperCase(),
          route: route.map(a => a.iataCode)
        }),
      });

      if (!res.ok) throw new Error(`HTTP ${res.status}`);
      setResult(await res.json());
    } catch (err) {
      setError(String(err));
    }
  }

  return (
    <div className="container">
      <header className="header">
        <h1>Travel Check</h1>
        <button
          className="secondary"
          onClick={() => setTheme(t => t === "light" ? "dark" : "light")}
        >
          {theme === "light" ? "🌙 Dark" : "☀️ Light"}
        </button>
      </header>

      <p className="subtitle">Baggage pickup & required documents</p>

      <form onSubmit={submit} className="card form">
        <label>
          Nationality
          <input
            value={nationality}
            onChange={e => setNationality(e.target.value)}
            placeholder="NO"
          />
        </label>

        <label className="autocomplete">
          Add airport
          <input
            value={query}
            onChange={e => setQuery(e.target.value)}
            placeholder="Search by name or IATA (e.g. Oslo / OSL)"
          />

          {suggestions.length > 0 && (
            <div className="dropdown">
              {suggestions.map(a => (
                <div
                  key={a.iataCode}
                  className="dropdown-item"
                  onClick={() => addAirport(a)}
                >
                  <b>{a.iataCode}</b> — {a.name}, {a.country}
                </div>
              ))}
            </div>
          )}
        </label>

        {route.length > 0 && (
          <div className="route">
            {route.map((a, i) => (
              <div key={a.iataCode} className="route-item">
                <span>{a.iataCode}</span>
                <button type="button" onClick={() => removeAirport(a.iataCode)}>✕</button>
                {i < route.length - 1 && <span className="arrow">→</span>}
              </div>
            ))}
          </div>
        )}

        <button type="submit">Check journey</button>
      </form>

      {error && <p className="error">{error}</p>}

      {result && (
        <div className="card result">
          <h2>Result</h2>

          <h3>Baggage pickup</h3>
          <p>Required: <b>{String(result.pickupRequired)}</b></p>
          <p>At: <b>{result.pickupAt.join(", ") || "None"}</b></p>

          <h3>Required documents</h3>
          <ul>
            {result.requiredDocuments.map(d => (
              <li key={d}>{d}</li>
            ))}
          </ul>
        </div>
      )}
    </div>
  );
}
