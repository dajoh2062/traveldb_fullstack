import { Moon, Sun } from "lucide-react";
import { useTranslation } from "react-i18next";
import LanguagePicker from "./LanguagePicker";

export default function AppHeader({ theme, onToggleTheme }) {
  const { t } = useTranslation();
  const ThemeIcon = theme === "light" ? Moon : Sun;

  return (
    <header className="topbar">
      <div className="topbar-inner">
        <a className="brand" href="#top" aria-label={t("header.homeLabel")}>
          <span className="brand-mark" aria-hidden="true">
            <svg viewBox="0 0 28 28">
              <path d="M5 20.5h5.5c2.1 0 3.5-1.1 3.5-3.3v-6.4c0-2.2 1.4-3.3 3.5-3.3H23" />
              <circle cx="5" cy="20.5" r="2.25" />
              <circle cx="23" cy="7.5" r="2.25" />
            </svg>
          </span>
          <span className="brand-wordmark">TravelDB</span>
        </a>
        <div className="topbar-actions">
          <LanguagePicker />
          <button
            aria-label={t(
              theme === "light" ? "header.switchToDarkMode" : "header.switchToLightMode",
            )}
            className="theme-toggle"
            onClick={onToggleTheme}
            type="button"
          >
            <ThemeIcon aria-hidden="true" className="icon" size={18} strokeWidth={1.8} />
          </button>
        </div>
      </div>
    </header>
  );
}
