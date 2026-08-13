export function normalizeSearch(value) {
  return value
    .normalize("NFD")
    .replace(/[\u0300-\u036f]/g, "")
    .trim()
    .toLowerCase();
}

export function countryDisplayName(country, locale = "en") {
  if (!country?.countryId) return country?.countryNameEn ?? "";

  try {
    return (
      new Intl.DisplayNames([locale], { type: "region" }).of(country.countryId) ??
      country.countryNameEn
    );
  } catch {
    return country.countryNameEn;
  }
}

const NO_COUNTRY_MATCH = 6;

export function buildCountrySearchIndex(countries, locale = "en") {
  return countries
    .map(country => {
      const displayName = countryDisplayName(country, locale);
      return {
        code: normalizeSearch(country.countryId),
        country,
        displayName,
        keywords: normalizeSearch(
          [country.countryNameEn, displayName, country.keywords].filter(Boolean).join(" "),
        ),
        name: normalizeSearch(displayName),
        sortName: displayName,
      };
    })
    .sort((left, right) => left.sortName.localeCompare(right.sortName, locale));
}

function countrySearchScore(country, query) {
  if (!query) return NO_COUNTRY_MATCH;
  if (country.code === query) return 0;
  if (country.code.startsWith(query)) return 1;
  if (country.name === query) return 2;
  if (country.name.startsWith(query)) return 3;
  if (country.name.includes(query)) return 4;
  if (country.keywords.includes(query)) return 5;
  return NO_COUNTRY_MATCH;
}

export function searchCountryIndex(countryIndex, query, limit = 8) {
  const normalizedQuery = normalizeSearch(query);
  if (!normalizedQuery) {
    return countryIndex.slice(0, limit).map(({ country }) => country);
  }

  return countryIndex
    .map(indexedCountry => ({
      indexedCountry,
      score: countrySearchScore(indexedCountry, normalizedQuery),
    }))
    .filter(({ score }) => score < NO_COUNTRY_MATCH)
    .sort((left, right) => left.score - right.score)
    .slice(0, limit)
    .map(({ indexedCountry }) => indexedCountry.country);
}
