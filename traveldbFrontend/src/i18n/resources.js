import ar from "./locales/ar.json";
import daDK from "./locales/da-DK.json";
import de from "./locales/de.json";
import enGB from "./locales/en-GB.json";
import enUS from "./locales/en-US.json";
import es from "./locales/es.json";
import fiFI from "./locales/fi-FI.json";
import fr from "./locales/fr.json";
import isIS from "./locales/is-IS.json";
import itIT from "./locales/it-IT.json";
import jaJP from "./locales/ja-JP.json";
import koKR from "./locales/ko-KR.json";
import nbNO from "./locales/nb-NO.json";
import nlBE from "./locales/nl-BE.json";
import nlNL from "./locales/nl-NL.json";
import pl from "./locales/pl.json";
import ptPT from "./locales/pt-PT.json";
import ru from "./locales/ru.json";
import svSE from "./locales/sv-SE.json";
import zhCN from "./locales/zh-CN.json";

const translations = {
  "zh-CN": zhCN,
  es,
  "en-GB": enGB,
  "en-US": enUS,
  "pt-PT": ptPT,
  ru,
  ar,
  pl,
  de,
  fr,
  "nl-NL": nlNL,
  "nb-NO": nbNO,
  "da-DK": daDK,
  "sv-SE": svSE,
  "fi-FI": fiFI,
  "is-IS": isIS,
  "nl-BE": nlBE,
  "it-IT": itIT,
  "ja-JP": jaJP,
  "ko-KR": koKR,
};

export const resources = Object.fromEntries(
  Object.entries(translations).map(([locale, translation]) => [locale, { translation }]),
);
