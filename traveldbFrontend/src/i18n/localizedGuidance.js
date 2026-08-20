import { resolveLocale } from "./locales";

function localizedText(i18n, locale, key, englishText, catalogIsCurrent = true) {
  const translatedText = catalogIsCurrent
    ? i18n.getResource(locale, "translation", key)
    : undefined;

  if (typeof translatedText === "string" && translatedText.trim()) {
    return { text: translatedText, lang: locale, isLocalized: true };
  }

  return {
    text: englishText ?? "",
    lang: "en",
    isLocalized: locale.startsWith("en"),
  };
}

function ruleIsPastReviewDate(reviewAfter) {
  const match = /^(\d{4})-(\d{2})-(\d{2})$/.exec(reviewAfter ?? "");
  if (!match) return false;

  const reviewDate = Date.UTC(Number(match[1]), Number(match[2]) - 1, Number(match[3]));
  const now = new Date();
  const today = Date.UTC(now.getUTCFullYear(), now.getUTCMonth(), now.getUTCDate());
  return reviewDate < today;
}

function guidanceCatalogIsCurrent(i18n, locale, document, datasetVersion) {
  if (document.ruleId?.startsWith("conservative-")) return true;
  if (ruleIsPastReviewDate(document.reviewAfter)) return false;
  return (
    typeof datasetVersion === "string" &&
    datasetVersion ===
      i18n.getResource(locale, "translation", "guidance.documentDatasetVersion")
  );
}

export function localizeDocumentRequirement(document, i18n, datasetVersion) {
  const locale = resolveLocale(i18n.resolvedLanguage ?? i18n.language);
  const baseKey = document.ruleId
    ? `guidance.documents.rules.${document.ruleId}`
    : null;
  const catalogIsCurrent =
    Boolean(baseKey) && guidanceCatalogIsCurrent(i18n, locale, document, datasetVersion);
  const title = localizedText(i18n, locale, `${baseKey}.title`, document.title, catalogIsCurrent);
  const summary = localizedText(
    i18n,
    locale,
    `${baseKey}.summary`,
    document.summary,
    catalogIsCurrent,
  );
  const conditions = (document.conditions ?? []).map((condition, index) =>
    localizedText(
      i18n,
      locale,
      `${baseKey}.conditions.${index}`,
      condition,
      catalogIsCurrent,
    ),
  );
  const keyFacts = (document.keyFacts ?? []).map((fact, index) => ({
    label: localizedText(
      i18n,
      locale,
      `${baseKey}.keyFacts.${index}.label`,
      fact.label,
      catalogIsCurrent,
    ),
    value: localizedText(
      i18n,
      locale,
      `${baseKey}.keyFacts.${index}.value`,
      fact.value,
      catalogIsCurrent,
    ),
  }));
  const translatedParts = [
    title,
    ...(document.summary ? [summary] : []),
    ...conditions,
    ...keyFacts.flatMap(fact => [fact.label, fact.value]),
  ];

  return {
    ...document,
    localized: { title, summary, conditions, keyFacts },
    isFullyLocalized: translatedParts.every(part => part.isLocalized),
  };
}

export function localizeBaggageAdvice(advice, i18n) {
  const locale = resolveLocale(i18n.resolvedLanguage ?? i18n.language);
  const baseKey = advice.adviceCode
    ? `guidance.baggage.advices.${advice.adviceCode}`
    : null;
  const title = localizedText(i18n, locale, `${baseKey}.title`, advice.title, Boolean(baseKey));
  const explanation = localizedText(
    i18n,
    locale,
    `${baseKey}.explanation`,
    advice.explanation,
    Boolean(baseKey),
  );

  return {
    ...advice,
    localized: { title, explanation },
    isFullyLocalized: [
      ...(advice.title ? [title] : []),
      ...(advice.explanation ? [explanation] : []),
    ].every(part => part.isLocalized),
  };
}
