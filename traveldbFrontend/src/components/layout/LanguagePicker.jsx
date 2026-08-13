import { Languages } from "lucide-react";
import { useTranslation } from "react-i18next";
import { resolveLocale, SUPPORTED_LOCALES } from "../../i18n/locales";

export default function LanguagePicker() {
  const { i18n, t } = useTranslation();
  const currentLocale = resolveLocale(i18n.resolvedLanguage ?? i18n.language);
  const label = t("language.label");

  return (
    <label className="language-picker" title={label}>
      <Languages aria-hidden="true" className="icon" size={17} strokeWidth={1.8} />
      <span className="visually-hidden">{label}</span>
      <select
        aria-label={label}
        onChange={event => i18n.changeLanguage(event.target.value)}
        value={currentLocale}
      >
        {SUPPORTED_LOCALES.map(locale => (
          <option dir={locale.direction} key={locale.code} lang={locale.code} value={locale.code}>
            {locale.label}
          </option>
        ))}
      </select>
    </label>
  );
}
