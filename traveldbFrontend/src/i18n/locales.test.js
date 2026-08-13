import { describe, expect, it } from "vitest";
import {
  DEFAULT_LOCALE,
  localeDirection,
  resolveLocale,
  SUPPORTED_LOCALE_CODES,
  SUPPORTED_LOCALES,
} from "./locales";
import { resources } from "./resources";

function leafEntries(value, prefix = "") {
  return Object.entries(value).flatMap(([key, nestedValue]) => {
    const path = prefix ? `${prefix}.${key}` : key;
    return nestedValue && typeof nestedValue === "object"
      ? leafEntries(nestedValue, path)
      : [[path, nestedValue]];
  });
}

function semanticKey(path) {
  return path.replace(/_(zero|one|two|few|many|other)$/, "_plural");
}

const PLURAL_KEYS = [
  "route.stops",
  "results.reviewItems",
  "results.baggage.actions",
  "results.documents.reviewCount",
  "results.documents.additionalConditions",
  "results.documents.missingInputs",
];

function nestedValue(object, path) {
  return path.split(".").reduce((value, key) => value?.[key], object);
}

describe("supported locales", () => {
  it("defines the complete language menu without duplicate codes", () => {
    expect(SUPPORTED_LOCALES).toHaveLength(20);
    expect(new Set(SUPPORTED_LOCALE_CODES)).toHaveProperty("size", 20);
    expect(SUPPORTED_LOCALE_CODES).toContain("en-GB");
    expect(SUPPORTED_LOCALE_CODES).toContain("en-US");
    expect(SUPPORTED_LOCALE_CODES).toContain("nl-BE");
    expect(SUPPORTED_LOCALE_CODES).toContain("ko-KR");
  });

  it("provides a translation resource for every supported locale", () => {
    expect(Object.keys(resources).sort()).toEqual([...SUPPORTED_LOCALE_CODES].sort());
    for (const locale of SUPPORTED_LOCALE_CODES) {
      expect(resources[locale].translation).toBeTypeOf("object");
    }
  });

  it("keeps every locale complete and free of blank translations", () => {
    const expectedKeys = [
      ...new Set(
        leafEntries(resources[DEFAULT_LOCALE].translation).map(([path]) => semanticKey(path)),
      ),
    ].sort();

    for (const locale of SUPPORTED_LOCALE_CODES) {
      const translation = resources[locale].translation;
      const entries = leafEntries(translation);
      expect([...new Set(entries.map(([path]) => semanticKey(path)))].sort(), locale).toEqual(
        expectedKeys,
      );
      for (const [, value] of entries) {
        expect(value.trim(), locale).not.toBe("");
      }
    }
  });

  it.each([
    ["en", "en-GB"],
    ["en-US", "en-US"],
    ["en-CA", "en-GB"],
    ["zh-Hans", "zh-CN"],
    ["pt-BR", "pt-PT"],
    ["no", "nb-NO"],
    ["nl-BE", "nl-BE"],
    ["ko", "ko-KR"],
    ["unknown", DEFAULT_LOCALE],
  ])("resolves %s to %s", (detected, expected) => {
    expect(resolveLocale(detected)).toBe(expected);
  });

  it("marks Arabic as right-to-left", () => {
    expect(localeDirection("ar")).toBe("rtl");
    expect(localeDirection("fr")).toBe("ltr");
  });

  it("keeps regional English wording distinct", () => {
    expect(resources["en-GB"].translation.planner.traveller.title).toBe("Traveller");
    expect(resources["en-US"].translation.planner.traveller.title).toBe("Traveler");
    expect(resources["en-GB"].translation.footer.disclaimer).toContain("travelling");
    expect(resources["en-US"].translation.footer.disclaimer).toContain("traveling");
  });

  it("provides every plural form required by each locale", () => {
    for (const locale of SUPPORTED_LOCALE_CODES) {
      const translation = resources[locale].translation;
      const categories = new Intl.PluralRules(locale).resolvedOptions().pluralCategories;

      for (const pluralKey of PLURAL_KEYS) {
        for (const category of categories) {
          expect(nestedValue(translation, `${pluralKey}_${category}`), locale).toBeTruthy();
        }
      }
    }
  });
});
