export function routeSummary(route) {
  return route.map(airport => airport.iataCode).join(" to ");
}

export function airportLabel(code, route) {
  const airport = route.find(item => item.iataCode === code);
  return airport ? `${airport.name} (${airport.iataCode})` : code;
}
