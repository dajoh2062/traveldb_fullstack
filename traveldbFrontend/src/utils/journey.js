export function routeSummary(route) {
  return route.map(airport => airport.iataCode).join(" → ");
}

export function airportLabel(code, route) {
  const airport = route.find(routeAirport => routeAirport.iataCode === code);
  return airport ? `${airport.name} (${airport.iataCode})` : code;
}
