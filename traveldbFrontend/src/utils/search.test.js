import { describe, expect, it } from "vitest";
import { buildCountrySearchIndex, normalizeSearch, searchCountryIndex } from "./search";

const countries = [
  { countryId: "GB", countryNameEn: "United Kingdom", keywords: "Britain England" },
  { countryId: "CI", countryNameEn: "Côte d'Ivoire", keywords: "Ivory Coast" },
  { countryId: "NO", countryNameEn: "Norway", keywords: "Norge" },
  { countryId: "NZ", countryNameEn: "New Zealand", keywords: "Aotearoa" },
];

describe("search utilities", () => {
  it("normalizes spacing, casing, and diacritics", () => {
    expect(normalizeSearch("  Côte D'IVOIRE ")).toBe("cote d'ivoire");
  });

  it("keeps country search ranking while using a precomputed index", () => {
    const index = buildCountrySearchIndex(countries);

    expect(searchCountryIndex(index, "no").map(country => country.countryId)).toEqual(["NO"]);
    expect(searchCountryIndex(index, "cote").map(country => country.countryId)).toEqual(["CI"]);
    expect(searchCountryIndex(index, "aotearoa").map(country => country.countryId)).toEqual(["NZ"]);
  });

  it("sorts an empty search alphabetically and respects its result limit", () => {
    const index = buildCountrySearchIndex(countries);

    expect(searchCountryIndex(index, "", 3).map(country => country.countryId)).toEqual([
      "CI",
      "NZ",
      "NO",
    ]);
  });
});
