import { mkdir, readFile, writeFile } from "node:fs/promises";
import { fileURLToPath } from "node:url";
import path from "node:path";

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
      `INSERT INTO ${table} (${columns.join(", ")}) VALUES\n${batch.map(row => `(${row.join(",")})`).join(",\n")};`
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

async function preserveAirlineSeed() {
  const airlineSeedPath = path.join(DATA_DIRECTORY, "airlines.sql");
  try {
    const existingAirlineSeed = await readFile(airlineSeedPath, "utf8");
    if (existingAirlineSeed.includes("INSERT INTO Airlines")) return;
  } catch {
    // First migration from the original monolithic seed file.
  }

  const legacySeedPath = path.join(RESOURCES_DIRECTORY, "data.sql");
  const legacySeed = await readFile(legacySeedPath, "utf8");
  const airlineInsert = legacySeed.match(/INSERT INTO Airlines[\s\S]*?;(?=\s*INSERT INTO Airports)/)?.[0];
  if (!airlineInsert) throw new Error("Could not find the airline seed block in data.sql");
  await writeFile(
    airlineSeedPath,
    `-- Curated airline seed data.\n${airlineInsert.trim()}\n`,
    "utf8"
  );
}

async function main() {
  const [countryRows, airportRows] = await Promise.all([
    downloadCsv(SOURCES.countries),
    downloadCsv(SOURCES.airports),
  ]);

  const countries = countryRows
    .filter(country => /^[A-Z]{2}$/.test(country.code))
    .sort((left, right) => left.name.localeCompare(right.name));
  const countriesByCode = new Map(countries.map(country => [country.code, country]));

  const airportByIata = new Map();
  for (const airport of airportRows) {
    const iataCode = airport.iata_code?.trim().toUpperCase();
    if (!/^[A-Z]{3}$/.test(iataCode) || airport.type === "closed" || !countriesByCode.has(airport.iso_country)) continue;
    airportByIata.set(iataCode, choosePreferredAirport(airportByIata.get(iataCode), airport));
  }
  const airports = [...airportByIata.values()].sort((left, right) => left.iata_code.localeCompare(right.iata_code));

  const countryValues = countries.map(country => [
    sqlNumber(country.id),
    sqlText(country.code),
    sqlText(country.name),
    sqlText(country.continent),
    sqlText(country.wikipedia_link),
    sqlText(country.keywords),
    sqlBoolean(SCHENGEN_COUNTRIES.has(country.code)),
  ]);

  const airportValues = airports.map(airport => {
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

  await mkdir(DATA_DIRECTORY, { recursive: true });
  await preserveAirlineSeed();

  const generatedAt = new Date().toISOString();
  await writeFile(
    path.join(DATA_DIRECTORY, "countries.sql"),
    `-- Generated ${generatedAt} from ${SOURCES.countries}\n-- OurAirports data is released into the public domain.\n${buildBatchedInserts("Countries", ["source_id", "country_id", "country_name_en", "continent", "wikipedia_url", "keywords", "is_schengen"], countryValues)}\n`,
    "utf8"
  );
  await writeFile(
    path.join(DATA_DIRECTORY, "airports.sql"),
    `-- Generated ${generatedAt} from ${SOURCES.airports}\n-- Includes every non-closed airport with a unique three-letter IATA code.\n-- OurAirports data is released into the public domain.\n${buildBatchedInserts("Airports", ["source_id", "ident", "iata_code", "icao_code", "gps_code", "local_code", "name", "municipality", "region_code", "country", "country_code", "continent", "airport_type", "scheduled_service", "latitude_deg", "longitude_deg", "elevation_ft", "official_url", "wikipedia_url", "keywords", "is_schengen"], airportValues)}\n`,
    "utf8"
  );
  await writeFile(
    path.join(DATA_DIRECTORY, "ourairports-metadata.json"),
    `${JSON.stringify({ generatedAt, sources: SOURCES, countryCount: countries.length, airportCount: airports.length, filter: "non-closed airports with a unique three-letter IATA code" }, null, 2)}\n`,
    "utf8"
  );

  console.log(`Generated ${countries.length} countries and ${airports.length} airports.`);
}

await main();
