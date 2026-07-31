export function routeSummary(route) {
  return route.map(airport => airport.iataCode).join(" to ");
}

export function airportLabel(code, route) {
  const airport = route.find(routeAirport => routeAirport.iataCode === code);
  return airport ? `${airport.name} (${airport.iataCode})` : code;
}

export function airportLocation(airport) {
  return airport.city ? `${airport.city} · ${airport.country}` : airport.country;
}
