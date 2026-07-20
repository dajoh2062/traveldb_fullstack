export function routeSummary(route) {
  return route.map(airport => airport.iataCode).join(" → ");
}

export function airportLabel(code, route) {
  const airport = route.find(item => item.iataCode === code);
  return airport ? `${airport.name} (${airport.iataCode})` : code;
}

export function routeStats(route) {
  const flightCount = Math.max(0, route.length - 1);
  const transitCount = Math.max(0, route.length - 2);
  const accessibilityLabel = [
    `${route.length} ${route.length === 1 ? "airport" : "airports"}`,
    `${flightCount} ${flightCount === 1 ? "flight" : "flights"}`,
    `${transitCount} ${transitCount === 1 ? "transit" : "transits"}`,
  ].join(", ");

  return { flightCount, transitCount, accessibilityLabel };
}
