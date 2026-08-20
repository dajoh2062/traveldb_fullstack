import { Moon, Plane, Sun } from "lucide-react";
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
            <Plane size={19} strokeWidth={2.2} />
          </span>
          <span className="brand-copy">
            <span className="brand-wordmark">TravelDB</span>
            <span className="brand-subtitle">Travel requirements</span>
          </span>
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
