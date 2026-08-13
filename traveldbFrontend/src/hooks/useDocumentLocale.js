import { useEffect } from "react";
import { useTranslation } from "react-i18next";
import { localeDirection, resolveLocale } from "../i18n/locales";

export default function useDocumentLocale() {
  const { i18n, t } = useTranslation();
  const locale = resolveLocale(i18n.resolvedLanguage ?? i18n.language);

  useEffect(() => {
    document.documentElement.lang = locale;
    document.documentElement.dir = localeDirection(locale);
    document.title = t("meta.title");

    const description = document.querySelector('meta[name="description"]');
    description?.setAttribute("content", t("meta.description"));
  }, [locale, t]);
}
