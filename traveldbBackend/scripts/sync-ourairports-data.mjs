import { mkdir, writeFile } from "node:fs/promises";
import { fileURLToPath } from "node:url";
import path from "node:path";
import process from "node:process";

const SCRIPT_DIRECTORY = path.dirname(fileURLToPath(import.meta.url));
const BACKEND_DIRECTORY = path.resolve(SCRIPT_DIRECTORY, "..");
const RESOURCES_DIRECTORY = path.join(BACKEND_DIRECTORY, "src", "main", "resources");
const DATA_DIRECTORY = path.join(RESOURCES_DIRECTORY, "data");

const SOURCES = {
  airports: "https://davidmegginson.github.io/ourairports-data/airports.csv",
  countries: "https://davidmegginson.github.io/ourairports-data/countries.csv",
};

const SCHENGEN_COUNTRIES = new Set([
  "AT", "BE", "BG", "CH", "CZ", "DE", "DK", "EE", "ES", "FI",
  "FR", "GR", "HR", "HU", "IS", "IT", "LI", "LT", "LU", "LV",
  "MT", "NL", "NO", "PL", "PT", "RO", "SE", "SI", "SK",
]);

const AIRPORT_TYPE_PRIORITY = {
  large_airport: 0,
  medium_airport: 1,
  small_airport: 2,
  seaplane_base: 3,
  heliport: 4,
  balloonport: 5,
};

function parseCsv(csv) {
  const rows = [];
  let row = [];
  let value = "";
  let quoted = false;

  for (let index = 0; index < csv.length; index += 1) {
    const character = csv[index];
    if (quoted) {
      if (character === '"' && csv[index + 1] === '"') {
        value += '"';
        index += 1;
      } else if (character === '"') {
        quoted = false;
      } else {
        value += character;
      }
    } else if (character === '"') {
      quoted = true;
    } else if (character === ",") {
      row.push(value);
      value = "";
    } else if (character === "\n") {
      row.push(value.replace(/\r$/, ""));
      rows.push(row);
      row = [];
      value = "";
    } else {
      value += character;
    }
  }

  if (value || row.length > 0) {
    row.push(value.replace(/\r$/, ""));
    rows.push(row);
  }

  const headers = rows.shift();
  return rows
    .filter(values => values.some(Boolean))
    .map(values => Object.fromEntries(headers.map((header, index) => [header, values[index] ?? ""])));
}

async function downloadCsv(url) {
  const response = await fetch(url, { headers: { "user-agent": "TravelDB data sync" } });
  if (!response.ok) throw new Error(`Could not download ${url}: ${response.status}`);
  return parseCsv(await response.text());
}

function sqlText(value) {
  const normalized = value?.trim();
  if (!normalized) return "NULL";
  return `'${normalized.replaceAll("'", "''")}'`;
}

function sqlNumber(value) {
  const normalized = value?.trim();
  return normalized && Number.isFinite(Number(normalized)) ? normalized : "NULL";
}

function sqlBoolean(value) {
  return value ? "TRUE" : "FALSE";
}

function buildBatchedInserts(table, columns, values, batchSize = 400) {
  const statements = [];
  for (let start = 0; start < values.length; start += batchSize) {
    const batch = values.slice(start, start + batchSize);
    statements.push(
      `INSERT INTO ${table} (${columns.join(", ")}) VALUES\n`
        + `${batch.map(row => `(${row.join(",")})`).join(",\n")};`,
    );
  }
  return statements.join("\n\n");
}

function choosePreferredAirport(current, candidate) {
  if (!current) return candidate;
  const currentScheduled = current.scheduled_service === "yes" ? 0 : 1;
  const candidateScheduled = candidate.scheduled_service === "yes" ? 0 : 1;
  if (candidateScheduled !== currentScheduled) return candidateScheduled < currentScheduled ? candidate : current;

  const currentType = AIRPORT_TYPE_PRIORITY[current.type] ?? 99;
  const candidateType = AIRPORT_TYPE_PRIORITY[candidate.type] ?? 99;
  if (candidateType !== currentType) return candidateType < currentType ? candidate : current;
  if (candidate.icao_code && !current.icao_code) return candidate;
  return Number(candidate.id) < Number(current.id) ? candidate : current;
}

function selectCountries(countryRows) {
  return countryRows
    .filter(country => /^[A-Z]{2}$/.test(country.code))
    .sort((left, right) => left.name.localeCompare(right.name));
}

function selectAirports(airportRows, countriesByCode) {
  const airportsByIata = new Map();
  for (const airport of airportRows) {
    const iataCode = airport.iata_code?.trim().toUpperCase();
    const hasKnownCountry = countriesByCode.has(airport.iso_country);
    if (!/^[A-Z]{3}$/.test(iataCode) || airport.type === "closed" || !hasKnownCountry) {
      continue;
    }

    const preferred = choosePreferredAirport(airportsByIata.get(iataCode), airport);
    airportsByIata.set(iataCode, preferred);
  }

  return [...airportsByIata.values()]
    .sort((left, right) => left.iata_code.localeCompare(right.iata_code));
}

function buildCountryRows(countries) {
  return countries.map(country => [
    sqlNumber(country.id),
    sqlText(country.code),
    sqlText(country.name),
    sqlText(country.continent),
    sqlText(country.wikipedia_link),
    sqlText(country.keywords),
    sqlBoolean(SCHENGEN_COUNTRIES.has(country.code)),
  ]);
}

function buildAirportRows(airports, countriesByCode) {
  return airports.map(airport => {
    const country = countriesByCode.get(airport.iso_country);
    return [
      sqlNumber(airport.id),
      sqlText(airport.ident),
      sqlText(airport.iata_code),
      sqlText(airport.icao_code),
      sqlText(airport.gps_code),
      sqlText(airport.local_code),
      sqlText(airport.name),
      sqlText(airport.municipality),
      sqlText(airport.iso_region),
      sqlText(country.name),
      sqlText(airport.iso_country),
      sqlText(airport.continent || country.continent),
      sqlText(airport.type),
      sqlBoolean(airport.scheduled_service === "yes"),
      sqlNumber(airport.latitude_deg),
      sqlNumber(airport.longitude_deg),
      sqlNumber(airport.elevation_ft),
      sqlText(airport.home_link),
      sqlText(airport.wikipedia_link),
      sqlText(airport.keywords),
      sqlBoolean(SCHENGEN_COUNTRIES.has(airport.iso_country)),
    ];
  });
}

async function writeGeneratedData(countries, airports, countriesByCode) {
  await mkdir(DATA_DIRECTORY, { recursive: true });

  const generatedAt = new Date().toISOString();
  const countryInserts = buildBatchedInserts(
    "Countries",
    ["source_id", "country_id", "country_name_en", "continent", "wikipedia_url", "keywords", "is_schengen"],
    buildCountryRows(countries),
  );
  const airportInserts = buildBatchedInserts(
    "Airports",
    [
      "source_id", "ident", "iata_code", "icao_code", "gps_code", "local_code",
      "name", "municipality", "region_code", "country", "country_code", "continent",
      "airport_type", "scheduled_service", "latitude_deg", "longitude_deg",
      "elevation_ft", "official_url", "wikipedia_url", "keywords", "is_schengen",
    ],
    buildAirportRows(airports, countriesByCode),
  );
  const metadata = {
    generatedAt,
    sources: SOURCES,
    countryCount: countries.length,
    airportCount: airports.length,
    filter: "non-closed airports with a unique three-letter IATA code",
  };

  await Promise.all([
    writeFile(
      path.join(DATA_DIRECTORY, "countries.sql"),
      `-- Generated ${generatedAt} from ${SOURCES.countries}\n`
        + `-- OurAirports data is released into the public domain.\n${countryInserts}\n`,
      "utf8",
    ),
    writeFile(
      path.join(DATA_DIRECTORY, "airports.sql"),
      `-- Generated ${generatedAt} from ${SOURCES.airports}\n`
        + "-- Includes every non-closed airport with a unique three-letter IATA code.\n"
        + `-- OurAirports data is released into the public domain.\n${airportInserts}\n`,
      "utf8",
    ),
    writeFile(
      path.join(DATA_DIRECTORY, "ourairports-metadata.json"),
      `${JSON.stringify(metadata, null, 2)}\n`,
      "utf8",
    ),
  ]);
}

async function main() {
  const [countryRows, airportRows] = await Promise.all([
    downloadCsv(SOURCES.countries),
    downloadCsv(SOURCES.airports),
  ]);
  const countries = selectCountries(countryRows);
  const countriesByCode = new Map(countries.map(country => [country.code, country]));
  const airports = selectAirports(airportRows, countriesByCode);

  await writeGeneratedData(countries, airports, countriesByCode);

  console.log(`Generated ${countries.length} countries and ${airports.length} airports.`);
}

try {
  await main();
} catch (error) {
  console.error(`OurAirports sync failed: ${error.message}`);
  process.exitCode = 1;
}
