import i18n from "i18next";
import LanguageDetector from "i18next-browser-languagedetector";
import { initReactI18next } from "react-i18next";
import {
  DEFAULT_LOCALE,
  findSupportedLocale,
  LANGUAGE_STORAGE_KEY,
  SUPPORTED_LOCALE_CODES,
} from "./locales";
import { resources } from "./resources";

if (!i18n.isInitialized) {
  i18n
    .use(LanguageDetector)
    .use(initReactI18next)
    .init({
      resources,
      fallbackLng: DEFAULT_LOCALE,
      supportedLngs: SUPPORTED_LOCALE_CODES,
      load: "currentOnly",
      interpolation: { escapeValue: false },
      react: { useSuspense: false },
      detection: {
        order: ["localStorage", "navigator"],
        caches: ["localStorage"],
        lookupLocalStorage: LANGUAGE_STORAGE_KEY,
        convertDetectedLanguage: detectedLanguage =>
          findSupportedLocale(detectedLanguage)?.code ?? detectedLanguage,
      },
    });
}

export default i18n;
