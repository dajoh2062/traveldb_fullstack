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
import guidanceAr from "./guidance/ar.json";
import guidanceDaDK from "./guidance/da-DK.json";
import guidanceDe from "./guidance/de.json";
import guidanceEnGB from "./guidance/en-GB.json";
import guidanceEnUS from "./guidance/en-US.json";
import guidanceEs from "./guidance/es.json";
import guidanceFiFI from "./guidance/fi-FI.json";
import guidanceFr from "./guidance/fr.json";
import guidanceIsIS from "./guidance/is-IS.json";
import guidanceItIT from "./guidance/it-IT.json";
import guidanceJaJP from "./guidance/ja-JP.json";
import guidanceKoKR from "./guidance/ko-KR.json";
import guidanceNbNO from "./guidance/nb-NO.json";
import guidanceNlBE from "./guidance/nl-BE.json";
import guidanceNlNL from "./guidance/nl-NL.json";
import guidancePl from "./guidance/pl.json";
import guidancePtPT from "./guidance/pt-PT.json";
import guidanceRu from "./guidance/ru.json";
import guidanceSvSE from "./guidance/sv-SE.json";
import guidanceZhCN from "./guidance/zh-CN.json";

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

const guidanceTranslations = {
  "zh-CN": guidanceZhCN,
  es: guidanceEs,
  "en-GB": guidanceEnGB,
  "en-US": guidanceEnUS,
  "pt-PT": guidancePtPT,
  ru: guidanceRu,
  ar: guidanceAr,
  pl: guidancePl,
  de: guidanceDe,
  fr: guidanceFr,
  "nl-NL": guidanceNlNL,
  "nb-NO": guidanceNbNO,
  "da-DK": guidanceDaDK,
  "sv-SE": guidanceSvSE,
  "fi-FI": guidanceFiFI,
  "is-IS": guidanceIsIS,
  "nl-BE": guidanceNlBE,
  "it-IT": guidanceItIT,
  "ja-JP": guidanceJaJP,
  "ko-KR": guidanceKoKR,
};

export const resources = Object.fromEntries(
  Object.entries(translations).map(([locale, translation]) => [
    locale,
    { translation: { ...translation, guidance: guidanceTranslations[locale] } },
  ]),
);
