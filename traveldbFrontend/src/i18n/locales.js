export const DEFAULT_LOCALE = "en-GB";
export const LANGUAGE_STORAGE_KEY = "traveldb-language";

export const SUPPORTED_LOCALES = [
  { code: "zh-CN", label: "中文（简体）", direction: "ltr" },
  { code: "es", label: "Español", direction: "ltr" },
  { code: "en-GB", label: "English (UK)", direction: "ltr" },
  { code: "en-US", label: "English (US)", direction: "ltr" },
  { code: "pt-PT", label: "Português", direction: "ltr" },
  { code: "ru", label: "Русский", direction: "ltr" },
  { code: "ar", label: "العربية", direction: "rtl" },
  { code: "pl", label: "Polski", direction: "ltr" },
  { code: "de", label: "Deutsch", direction: "ltr" },
  { code: "fr", label: "Français", direction: "ltr" },
  { code: "nl-NL", label: "Nederlands", direction: "ltr" },
  { code: "nb-NO", label: "Norsk bokmål", direction: "ltr" },
  { code: "da-DK", label: "Dansk", direction: "ltr" },
  { code: "sv-SE", label: "Svenska", direction: "ltr" },
  { code: "fi-FI", label: "Suomi", direction: "ltr" },
  { code: "is-IS", label: "Íslenska", direction: "ltr" },
  { code: "nl-BE", label: "Nederlands (België)", direction: "ltr" },
  { code: "it-IT", label: "Italiano", direction: "ltr" },
  { code: "ja-JP", label: "日本語", direction: "ltr" },
  { code: "ko-KR", label: "한국어", direction: "ltr" },
];

export const SUPPORTED_LOCALE_CODES = SUPPORTED_LOCALES.map(locale => locale.code);

const localeByLowercaseCode = new Map(
  SUPPORTED_LOCALES.map(locale => [locale.code.toLowerCase(), locale]),
);

function normalizedLanguageTag(value) {
  if (typeof value !== "string" || !value.trim()) return "";

  const candidate = value.trim().replaceAll("_", "-");
  try {
    return Intl.getCanonicalLocales(candidate)[0] ?? "";
  } catch {
    return candidate;
  }
}

export function findSupportedLocale(value) {
  const tag = normalizedLanguageTag(value);
  if (!tag) return null;

  const exactLocale = localeByLowercaseCode.get(tag.toLowerCase());
  if (exactLocale) return exactLocale;

  const language = tag.split("-")[0].toLowerCase();
  if (language === "en") {
    return tag.toLowerCase() === "en-us"
      ? localeByLowercaseCode.get("en-us")
      : localeByLowercaseCode.get("en-gb");
  }
  if (language === "nl") {
    return tag.toLowerCase() === "nl-be"
      ? localeByLowercaseCode.get("nl-be")
      : localeByLowercaseCode.get("nl-nl");
  }

  const aliases = {
    ar: "ar",
    da: "da-DK",
    de: "de",
    es: "es",
    fi: "fi-FI",
    fr: "fr",
    is: "is-IS",
    it: "it-IT",
    ja: "ja-JP",
    ko: "ko-KR",
    nb: "nb-NO",
    no: "nb-NO",
    pl: "pl",
    pt: "pt-PT",
    ru: "ru",
    sv: "sv-SE",
    zh: "zh-CN",
  };

  const alias = aliases[language];
  return alias ? localeByLowercaseCode.get(alias.toLowerCase()) : null;
}

export function resolveLocale(value) {
  return findSupportedLocale(value)?.code ?? DEFAULT_LOCALE;
}

export function localeDirection(value) {
  return findSupportedLocale(value)?.direction ?? "ltr";
}
