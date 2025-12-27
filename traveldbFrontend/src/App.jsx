import { useState } from "react";
import "./App.css";

export default function App() {
  const [nationality, setNationality] = useState("NO");
  const [routeText, setRouteText] = useState("OSL,FRA,JFK");
  const [result, setResult] = useState(null);
  const [error, setError] = useState("");

  async function submit(e) {
    e.preventDefault();
    setError("");
    setResult(null);

    const route = routeText
      .split(",")
      .map(s => s.trim().toUpperCase())
      .filter(Boolean);

    try {
      const res = await fetch("/api/journey/check", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          nationalityCountryCode: nationality.trim().toUpperCase(),
          route
        }),
      });

      if (!res.ok) throw new Error(`HTTP ${res.status}`);
      const data = await res.json();
      setResult(data);
    } catch (err) {
      setError(String(err));
    }
  }

  return (
    <div style={{ maxWidth: 720, margin: "40px auto", padding: 16 }}>
      <h1>Travel Check</h1>
      <p>Checks (1) baggage pickup during transit, (2) required documents.</p>

      <form onSubmit={submit} style={{ display: "grid", gap: 12 }}>
        <label>
          Nationality (2-letter code):
          <input
            value={nationality}
            onChange={(e) => setNationality(e.target.value)}
            placeholder="NO"
            style={{ width: "100%", padding: 10, marginTop: 6 }}
          />
        </label>

        <label>
          Route (IATA codes, comma-separated):
          <input
            value={routeText}
            onChange={(e) => setRouteText(e.target.value)}
            placeholder="OSL,FRA,JFK"
            style={{ width: "100%", padding: 10, marginTop: 6 }}
          />
        </label>

        <button type="submit" style={{ padding: 12, fontSize: 16 }}>
          Check journey
        </button>
      </form>

      {error && <p style={{ marginTop: 16 }}>Error: {error}</p>}

      {result && (
        <div style={{ marginTop: 24, padding: 16, border: "1px solid #ccc", borderRadius: 8 }}>
          <h2>Result</h2>

          <h3>Baggage pickup</h3>
          <p>
            Pickup required: <b>{String(result.pickupRequired)}</b>
          </p>
          <p>
            Pickup at: <b>{(result.pickupAt || []).join(", ") || "None"}</b>
          </p>

          <h3>Required documents</h3>
          <ul>
            {(result.requiredDocuments || []).map((d) => (
              <li key={d}>{d}</li>
            ))}
          </ul>
        </div>
      )}
    </div>
  );
}
